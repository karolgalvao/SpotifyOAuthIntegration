package com.spotify.integration.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PKCEUtil {

    public String generateCodeVerifier() {

        byte[] codeVerifier = new byte[32];
        new SecureRandom().nextBytes(codeVerifier);

        return Base64.
                getUrlEncoder().
                withoutPadding().
                encodeToString(codeVerifier);
    }

    public String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(codeVerifier.getBytes());

            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("SHA-256 algorithm not available", exception);
        }
    }
}
