package com.spotify.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.integration.exception.SpotifyAuthException;
import com.spotify.integration.util.PKCEUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class SpotifyAuthService {

    private final PKCEUtil pkceUtil;

    private final WebClient webClient;

    private Instant tokenExpirationTime;

    @Value("${spotify.client_id}")
    private String clientId;

    @Value("${spotify.client_secret}")
    private String clientSecret;

    @Value("${spotify.redirect_uri}")
    private String redirectUri;

    private String accessToken;

    private String refreshToken;

    public SpotifyAuthService(PKCEUtil pkceUtil) {
        this.pkceUtil = pkceUtil;
        this.webClient = WebClient.create();
    }

    public boolean isTokenExpired() {
        return tokenExpirationTime == null || Instant.now().isAfter(tokenExpirationTime);
    }

    public Map<String, String> exchangeAuthorizationCodeForToken(String code, String codeVerifier) throws SpotifyAuthException {
        try {
            String responseBody = webClient.post()
                    .uri("https://accounts.spotify.com/api/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(
                            "grant_type=authorization_code&" +
                                    "code=" + code + "&" +
                                    "redirect_uri=" + redirectUri + "&" +
                                    "client_id=" + clientId + "&" +
                                    "client_secret=" + clientSecret + "&" +
                                    "code_verifier=" + codeVerifier
                    )
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {});

            Map<String, String> stringResponse = new HashMap<>();
            for (Map.Entry<String, Object> entry : response.entrySet()) {
                stringResponse.put(entry.getKey(), String.valueOf(entry.getValue()));
            }

            if (stringResponse.containsKey("access_token")) {
                accessToken = stringResponse.get("access_token");
                refreshToken = stringResponse.getOrDefault("refresh_token", refreshToken);
                int expiresIn = Integer.parseInt(stringResponse.get("expires_in"));
                tokenExpirationTime = Instant.now().plusSeconds(expiresIn);

                return stringResponse;
            } else {
                throw new SpotifyAuthException("Invalid response: access_token not found.");
            }
        } catch (WebClientResponseException e) {
            throw new SpotifyAuthException("Failed to exchange authorization code for token: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SpotifyAuthException("Unexpected error during token exchange", e);
        }
    }

    public String generateSpotifyAuthUrl() {
        String codeVerifier = pkceUtil.generateCodeVerifier();
        String codeChallenge = pkceUtil.generateCodeChallenge(codeVerifier);

        System.out.println("Generated Code Verifier: " + codeVerifier);

        return "https://accounts.spotify.com/authorize" +
                "?client_id=" + clientId +
                "&response_type=code" +
                "&redirect_uri=" + redirectUri +
                "&code_challenge=" + codeChallenge +
                "&code_challenge_method=S256" +
                "&scope=user-read-private user-read-email" +
                "&state=" + codeVerifier;
    }

    public String refreshAccessToken(String refreshToken) throws SpotifyAuthException {
        try {
            String responseBody = webClient.post()
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

                System.out.println("Access token successfully refreshed.");
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
        if (refreshToken != null && isTokenExpired()) {
            try {
                refreshAccessToken(refreshToken);
                System.out.println("Access token automatically refreshed at " + Instant.now());
            } catch (SpotifyAuthException e) {
                System.err.println("Failed to refresh access token: " + e.getMessage());
            }
        } else {
            System.out.println("Token is still valid, no refresh needed.");
        }
    }
}