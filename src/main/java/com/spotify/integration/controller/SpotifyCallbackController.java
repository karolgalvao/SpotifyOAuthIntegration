package com.spotify.integration.controller;

import com.spotify.integration.service.SpotifyAuthService;
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
    public Map<String, String> handleSpotifyCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            @RequestParam(value = "error", required = false) String error) {

        if (error != null) {
            return Map.of("error", "Authorization failed: " + error);
        }

        if (state == null || state.isEmpty()) {
            return Map.of("error", "State (code verifier) is missing or invalid.");
        }

        try {
            Map<String, String> tokenResponse = spotifyAuthService.exchangeAuthorizationCodeForToken(code, state);
            return tokenResponse;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
