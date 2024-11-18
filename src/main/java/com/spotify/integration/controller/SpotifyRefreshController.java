package com.spotify.integration.controller;

import com.spotify.integration.service.SpotifyAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpotifyRefreshController {

    private final SpotifyAuthService spotifyAuthService;

    public SpotifyRefreshController(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    @PostMapping("/refresh-token")
    public String refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return spotifyAuthService.refreshAccessToken(refreshToken);
    }
}
