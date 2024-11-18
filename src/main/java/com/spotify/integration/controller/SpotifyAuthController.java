package com.spotify.integration.controller;

import com.spotify.integration.service.SpotifyAuthService;
import jakarta.servlet.http.HttpSession;
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
    public RedirectView authenticate(HttpSession httpSession) {
        String spotifyAuthUrl = spotifyAuthService.generateSpotifyAuthUrl(httpSession);

        return new RedirectView(spotifyAuthUrl);
    }

}
