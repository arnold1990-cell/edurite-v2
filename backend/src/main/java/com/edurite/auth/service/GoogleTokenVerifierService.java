package com.edurite.auth.service;

import com.edurite.auth.config.GoogleSignInProperties;
import com.edurite.auth.dto.GoogleIdentity;
import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.common.exception.ResourceConflictException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifierService {

    private static final String DEFAULT_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final GoogleSignInProperties googleSignInProperties;
    private final OkHttpClient httpClient;

    public GoogleTokenVerifierService(GoogleSignInProperties googleSignInProperties) {
        this.googleSignInProperties = googleSignInProperties;
        this.httpClient = new OkHttpClient();
    }

    public GoogleIdentity verifyIdToken(String idToken) {
        if (!googleSignInProperties.enabled()) {
            throw new ResourceConflictException("Google sign-in is currently disabled.");
        }

        String configuredClientId = trimToNull(googleSignInProperties.clientId());
        if (configuredClientId == null) {
            throw new ResourceConflictException("Google sign-in is not configured on this server.");
        }

        String token = trimToNull(idToken);
        if (token == null) {
            throw new InvalidCredentialsException("Unable to sign in with Google.");
        }

        String tokenInfoUrl = trimToNull(googleSignInProperties.tokenInfoUrl());
        HttpUrl requestUrl = HttpUrl.parse(tokenInfoUrl == null ? DEFAULT_TOKEN_INFO_URL : tokenInfoUrl)
                .newBuilder()
                .addQueryParameter("id_token", token)
                .build();

        Request request = new Request.Builder().url(requestUrl).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new InvalidCredentialsException("Unable to sign in with Google.");
            }

            JsonObject payload = JsonParser.parseString(response.body().string()).getAsJsonObject();
            String audience = asString(payload, "aud");
            String email = asString(payload, "email");
            boolean emailVerified = asBoolean(payload, "email_verified");
            long expEpochSeconds = asLong(payload, "exp");

            long nowEpochSeconds = System.currentTimeMillis() / 1000;
            if (!configuredClientId.equals(audience) || email == null || !emailVerified || expEpochSeconds <= nowEpochSeconds) {
                throw new InvalidCredentialsException("Unable to sign in with Google.");
            }

            String givenName = asString(payload, "given_name");
            String familyName = asString(payload, "family_name");
            String fullName = asString(payload, "name");
            String[] names = splitName(givenName, familyName, fullName, email);

            return new GoogleIdentity(email, names[0], names[1], fullName == null ? (names[0] + " " + names[1]).trim() : fullName);
        } catch (InvalidCredentialsException ex) {
            throw ex;
        } catch (RuntimeException | java.io.IOException ex) {
            throw new InvalidCredentialsException("Unable to sign in with Google.");
        }
    }

    private String asString(JsonObject payload, String key) {
        JsonElement element = payload.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        String value = element.getAsString();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean asBoolean(JsonObject payload, String key) {
        JsonElement element = payload.get(key);
        if (element == null || element.isJsonNull()) {
            return false;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        return "true".equalsIgnoreCase(element.getAsString());
    }

    private long asLong(JsonObject payload, String key) {
        JsonElement element = payload.get(key);
        if (element == null || element.isJsonNull()) {
            return 0L;
        }
        try {
            return element.getAsLong();
        } catch (RuntimeException ex) {
            return 0L;
        }
    }

    private String[] splitName(String givenName, String familyName, String fullName, String email) {
        String resolvedGivenName = trimToNull(givenName);
        String resolvedFamilyName = trimToNull(familyName);
        if (resolvedGivenName != null && resolvedFamilyName != null) {
            return new String[]{resolvedGivenName, resolvedFamilyName};
        }

        String fromFullName = trimToNull(fullName);
        if (fromFullName != null) {
            String[] parts = fromFullName.split("\\s+", 2);
            if (parts.length == 2) {
                return new String[]{parts[0], parts[1]};
            }
            return new String[]{parts[0], "User"};
        }

        String localPart = email == null ? null : email.split("@", 2)[0];
        if (localPart != null && !localPart.isBlank()) {
            return new String[]{localPart, "User"};
        }
        return new String[]{"EduRite", "User"};
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

