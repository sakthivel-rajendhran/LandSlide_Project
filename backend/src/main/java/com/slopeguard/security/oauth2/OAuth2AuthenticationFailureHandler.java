package com.slopeguard.security.oauth2;

import com.slopeguard.util.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

/**
 * Handles OAuth2 authentication failures, cleans up state cookies, and redirects safely
 * to the frontend login interface with a sanitized, user-friendly error indicator.
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Value("${slopeguard.frontend.url:http://localhost:5500}")
    private String defaultFrontendUrl;

    public OAuth2AuthenticationFailureHandler(HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository) {
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 authentication failed: {}", exception.getMessage());

        String targetUrl = resolveFrontendBaseUrl(request) + "/login.html";
        String errorCode = "oauth_failed";

        String msg = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        if (msg.contains("access_denied") || msg.contains("consent_required") || msg.contains("cancel")) {
            errorCode = "oauth_canceled";
        } else if (msg.contains("client_id") || msg.contains("client_secret") || msg.contains("invalid_client")) {
            errorCode = "oauth_config_error";
        }

        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", errorCode)
                .build().toUriString();

        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String resolveFrontendBaseUrl(HttpServletRequest request) {
        Optional<String> redirectParam = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(jakarta.servlet.http.Cookie::getValue);

        if (redirectParam.isPresent() && !redirectParam.get().isBlank()) {
            return redirectParam.get().replaceAll("/+$", "");
        }

        return defaultFrontendUrl.replaceAll("/+$", "");
    }
}

