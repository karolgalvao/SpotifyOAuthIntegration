package com.spotify.integration.controller;

import com.spotify.integration.service.SpotifyAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SpotifyCallbackController {

    private final SpotifyAuthService spotifyAuthService;

    public SpotifyCallbackController(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    @GetMapping("/auth/callback")
    public Map<String, String> handleSpotifyCallback (
            @RequestParam("code") String code,
            @RequestParam(value = "error", required = false) String error,
            HttpSession httpSession) {

        if (error != null) {
            return Map.of("error", "Authorization failed: " + error);
        }

        String codeVerifier = (String) httpSession.getAttribute("codeVerifier");

        if (codeVerifier == null) {
            return Map.of("error", "Session expired or code verifier missing.");
        }

        try {
            String accessToken = spotifyAuthService.exchangeAuthorizationCodeForToken(code, codeVerifier);
            return Map.of("accessToken", accessToken);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
