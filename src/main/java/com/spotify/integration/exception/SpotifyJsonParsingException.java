package com.spotify.integration.exception;

import com.fasterxml.jackson.core.JsonProcessingException;

public class SpotifyJsonParsingException extends SpotifyAuthException {
    public SpotifyJsonParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
