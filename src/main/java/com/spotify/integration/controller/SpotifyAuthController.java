package com.spotify.integration.controller;

import com.spotify.integration.service.SpotifyAuthService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class SpotifyAuthController {

    private final SpotifyAuthService spotifyAuthService;

    public SpotifyAuthController(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    @GetMapping("auth/spotify")
    public RedirectView authenticate() {
        String spotifyAuthUrl = spotifyAuthService.generateSpotifyAuthUrl();

        return new RedirectView(spotifyAuthUrl);
    }
}
