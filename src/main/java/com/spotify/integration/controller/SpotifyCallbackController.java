package com.spotify.integration.controller;

import com.spotify.integration.dto.TokenResponse;
import com.spotify.integration.exception.SpotifyAuthException;
import com.spotify.integration.service.SpotifyAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> handleSpotifyCallback(@RequestParam("code") String code,
                                                   @RequestParam("state") String state,
                                                   @RequestParam(value = "error", required = false) String error) {

        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Authorization failed: " + error));
        }

        if (state == null || state.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "State (code verifier) is missing or invalid."));
        }

        try {
            TokenResponse tokenResponse = spotifyAuthService.exchangeAuthorizationCodeForToken(code, state);
            return ResponseEntity.ok(tokenResponse);
        } catch (SpotifyAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred."));
        }
    }
}
