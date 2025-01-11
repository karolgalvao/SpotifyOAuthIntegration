package com.spotify.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.integration.dto.TokenResponse;
import com.spotify.integration.exception.SpotifyAuthException;
import com.spotify.integration.exception.SpotifyJsonParsingException;
import com.spotify.integration.exception.SpotifyResourceNotFoundException;
import com.spotify.integration.util.PKCEUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class SpotifyAuthService {

    private final static Logger logger = LoggerFactory.getLogger(SpotifyAuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PKCEUtil pkceUtil;
    private final WebClient spotifyWebClient;
    private Instant tokenExpirationTime;

    @Value("${spotify.client_id}")
    private String clientId;

    @Value("${spotify.client_secret}")
    private String clientSecret;

    @Value("${spotify.redirect_uri}")
    private String redirectUri;

    private String accessToken;

    private String refreshToken;

    public SpotifyAuthService(PKCEUtil pkceUtil, WebClient spotifyWebClient) {
        this.pkceUtil = pkceUtil;
        this.spotifyWebClient = spotifyWebClient;
    }

    public boolean isTokenExpired() {
        return tokenExpirationTime == null || Instant.now().isAfter(tokenExpirationTime);
    }

    private <T> T _parseResponse(String responseBody, Object type) {
        try {
            if (type instanceof Class<?>) {
                return objectMapper.readValue(responseBody, (Class<T>) type);
            } else if (type instanceof TypeReference<?>) {
                return objectMapper.readValue(responseBody, (TypeReference<T>) type);
            } else {
                throw new IllegalArgumentException("Tipo inválido para parsing: deve ser Class ou TypeReference.");
            }
        } catch (JsonProcessingException e) {
            String targetType = (type instanceof Class<?>) ? ((Class<?>) type).getSimpleName() : type.toString();
            String errorMessage = "Erro ao processar resposta JSON para " + targetType + ": " + e.getMessage();
            logger.error(errorMessage, e);
            throw new SpotifyJsonParsingException(errorMessage, e);
        }
    }

    private void _handleWebClientResponseException(WebClientResponseException e) {
        String errorMessage = "Erro na API do Spotify: " + e.getResponseBodyAsString();
        logger.error(errorMessage, e);

        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new SpotifyResourceNotFoundException("Recurso não encontrado.", e);
        } else if (e.getStatusCode().is4xxClientError()){
            throw new SpotifyAuthException("Requisição inválida: " + e.getResponseBodyAsString(), e);
        } else {
            throw new SpotifyAuthException("Falha na API do Spotify: " + e.getResponseBodyAsString(), e);
        }
    }

    private void _updateTokens(TokenResponse response) {
        this.accessToken = response.getAccessToken();
        this.refreshToken = response.getRefreshToken();
        this.tokenExpirationTime = Instant.now().plusSeconds(response.getExpiresIn());
    }

    public TokenResponse exchangeAuthorizationCodeForToken(String code, String codeVerifier) {
        String requestBody = "grant_type=authorization_code&" +
                "code=" + code + "&" +
                "redirect_uri=" + redirectUri + "&" +
                "client_id=" + clientId + "&" +
                "client_secret=" + clientSecret + "&" +
                "code_verifier=" + codeVerifier;

        String responseBody = spotifyWebClient.post()
                .uri("https://accounts.spotify.com/api/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        TokenResponse response = _parseResponse(responseBody, TokenResponse.class);
        _updateTokens(response);

        return response;
    }

    public Map<String, Object> getUserPlaylists() {
        String validAccessToken = getValidAccessToken();
        try {
            String responseBody = spotifyWebClient.get()
                    .uri("https://api.spotify.com/v1/me/playlists")
                    .header("Authorization", "Bearer " + validAccessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return _parseResponse(responseBody, new TypeReference<>() {});
        } catch (WebClientResponseException e) {
            _handleWebClientResponseException(e);
        }
        return null;
    }

    public Map<String, Object> getUserProfile() {
        String validAccessToken = getValidAccessToken();
        try {
            String responseBody = spotifyWebClient.get()
                    .uri("https://api.spotify.com/v1/me")
                    .header("Authorization", "Bearer " + validAccessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return _parseResponse(responseBody, new TypeReference<>() {});
        } catch (WebClientResponseException e) {
            _handleWebClientResponseException(e);
        }
        return null;
    }

    public List<Map<String, Object>> getUserTopTracks() {
        String validAccessToken = getValidAccessToken();
        String endpoint = "https://api.spotify.com/v1/me/top/tracks";
        logger.debug("Endpoint da API do Spotify chamado: {}", endpoint);

        try {
            String responseBody = spotifyWebClient.get()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validAccessToken) // Usando HttpHeaders.AUTHORIZATION
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logger.trace("Resposta (truncada) do Spotify: {}", responseBody.substring(0, Math.min(responseBody.length(), 200)));

            TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
            Map<String, Object> responseMap = _parseResponse(responseBody, typeRef);

            List<Map<String, Object>> tracks = (List<Map<String, Object>>) responseMap.get("items");

            if (tracks == null) {
                logger.warn("A resposta da API não continha a chave 'items'.");
                return Collections.emptyList();
            }

            return tracks;

        } catch (WebClientResponseException e) {
            logger.error("Erro na API do Spotify: Status: {}, Corpo: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            _handleWebClientResponseException(e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Erro interno ao processar top tracks: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public String getValidAccessToken() {
        if (isTokenExpired()) {
            refreshAccessToken(refreshToken);
        }
        return accessToken;
    }

    public String generateSpotifyAuthUrl() {
        String codeVerifier = pkceUtil.generateCodeVerifier();
        String codeChallenge = pkceUtil.generateCodeChallenge(codeVerifier);

        return "https://accounts.spotify.com/authorize" +
                "?client_id=" + clientId +
                "&response_type=code" +
                "&redirect_uri=" + redirectUri +
                "&code_challenge=" + codeChallenge +
                "&code_challenge_method=S256" +
                "&scope=user-read-private user-read-email playlist-read-private playlist-modify-public user-top-read" +
                "&state=" + codeVerifier;
    }

    public String refreshAccessToken(String refreshToken) throws SpotifyAuthException {
        try {
            String responseBody = spotifyWebClient.post()
                    .uri("https://accounts.spotify.com/api/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(
                            "grant_type=refresh_token&" +
                                    "refresh_token=" + refreshToken + "&" +
                                    "client_id=" + clientId + "&" +
                                    "client_secret=" + clientSecret
                    )
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> response = objectMapper.readValue(responseBody, new TypeReference<>() {});

            if (response.containsKey("access_token")) {
                accessToken = response.get("access_token");
                int expiresIn = Integer.parseInt(response.get("expires_in"));
                tokenExpirationTime = Instant.now().plusSeconds(expiresIn);

                return accessToken;
            } else {
                throw new SpotifyAuthException("Failed to refresh access token: access_token not found.");
            }
        } catch (WebClientResponseException e) {
            throw new SpotifyAuthException("Failed to refresh token: " + e.getResponseBodyAsString(), e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse response while refreshing token.", e);
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void autoRefreshAccessToken() {
        if (isTokenExpired() && refreshToken != null) {
            try {
                refreshAccessToken(refreshToken);
            } catch (SpotifyAuthException e) {
                logger.error("Failed to refresh access token: {}", e.getMessage());
            }
        } else {
            logger.debug("Token is still valid, no refresh needed.");
        }
    }
}