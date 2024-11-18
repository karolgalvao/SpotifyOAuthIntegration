package com.spotify.integration.controller;

import com.spotify.integration.exception.SpotifyAuthException;
import com.spotify.integration.service.SpotifyAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SpotifyUserController {

    private final SpotifyAuthService spotifyAuthService;

    public SpotifyUserController(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    @GetMapping("/user/profile")
    public Map<String, Object> getUserProfile() {
        try {
            return spotifyAuthService.getUserProfile();
        } catch (SpotifyAuthException e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/user/playlists")
    public Map<String, Object> getUserPlaylists() {
        try {
            return spotifyAuthService.getUserPlaylists();
        } catch (SpotifyAuthException e) {
            return Map.of("error", e.getMessage());
        }
    }
}
