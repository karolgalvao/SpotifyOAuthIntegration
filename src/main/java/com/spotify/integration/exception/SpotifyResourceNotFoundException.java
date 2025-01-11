package com.spotify.integration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SpotifyResourceNotFoundException extends SpotifyAuthException {

    public SpotifyResourceNotFoundException(String message) {
        super(message);
    }

    public SpotifyResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
