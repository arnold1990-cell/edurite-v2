package com.edurite.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class GoogleOAuthCompatibilityController {

    private final String frontendUrl;

    public GoogleOAuthCompatibilityController(
            @Value("${app.frontend-url:${app.frontend.base-url:http://localhost:5173}}") String frontendUrl
    ) {
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/oauth2/authorization/google")
    RedirectView googleAuthorization() {
        return redirectToLogin("google");
    }

    @GetMapping("/login/oauth2/code/google")
    RedirectView googleCallback(@RequestParam(name = "error", required = false) String error) {
        return redirectToLogin(error == null || error.isBlank() ? "google-callback" : "google-error");
    }

    private RedirectView redirectToLogin(String source) {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/auth/login")
                .queryParam("oauth", source)
                .build()
                .toUriString();
        RedirectView redirectView = new RedirectView(redirectUrl);
        redirectView.setStatusCode(org.springframework.http.HttpStatus.FOUND);
        return redirectView;
    }
}

