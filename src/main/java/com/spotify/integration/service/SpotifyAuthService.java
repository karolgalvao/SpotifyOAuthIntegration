package com.spotify.integration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.integration.exception.SpotifyAuthException;
import com.spotify.integration.util.PKCEUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
public class SpotifyAuthService {

    @Value("${spotify.client_id}")
    private String clientId;

    @Value("${spotify.client_secret}")
    private String clientSecret;

    @Value("${spotify.redirect_uri}")
    private String redirectUri;

    private final PKCEUtil pkceUtil;

    private final WebClient webClient;

    public SpotifyAuthService(PKCEUtil pkceUtil) {
        this.pkceUtil = pkceUtil;
        this.webClient = WebClient.create();
    }

    public String generateSpotifyAuthUrl(HttpSession httpSession) {

        String codeVerifier = pkceUtil.generateCodeVerifier();
        String codeChallenge = pkceUtil.generateCodeChallenge(codeVerifier);

        httpSession.setAttribute("codeVerifier", codeVerifier);

        return "https://accounts.spotify.com/authorize" +
                "?client_id=" + clientId +
                "&response_type=code" +
                "&redirect_uri=" + redirectUri +
                "&code_challenge=" + codeChallenge +
                "&code_challenge_method=S256" +
                "&scope=user-read-private user-read-email";
    }

    public String exchangeAuthorizationCodeForToken(String code, String codeVerifier) throws SpotifyAuthException {
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
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

            if (response.containsKey("access_token")) {
                return (String) response.get("access_token");
            } else {
                throw new SpotifyAuthException("Invalid response: access_token not found");
            }
        } catch (WebClientResponseException e) {
            throw new SpotifyAuthException("Failed to exchange authorization code for access token: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new SpotifyAuthException("Unexpected error during token exchange", e);
        }
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
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

            if (response.containsKey("access_token")) {
                return (String) response.get("access_token");
            } else {
                throw new SpotifyAuthException("Invalid response: access_token not found during refresh");
            }
        } catch (WebClientResponseException e) {
            throw new SpotifyAuthException("Failed to refresh access token: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new SpotifyAuthException("Unexpected error during token refresh", e);
        }
    }


}