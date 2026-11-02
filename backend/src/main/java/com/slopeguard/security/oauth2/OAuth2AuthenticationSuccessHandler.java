package com.slopeguard.security.oauth2;

import com.slopeguard.exception.OAuthEmailConflictException;
import com.slopeguard.model.User;
import com.slopeguard.service.JwtService;
import com.slopeguard.service.UserService;
import com.slopeguard.util.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Handles successful OAuth2 / OpenID Connect authentication from Google.
 * <p>
 * Security Guarantees:
 * 1. Validates verified email claim from Google OIDC token.
 * 2. Enforces existing account conflict rules (prevents unauthenticated account takeover).
 * 3. Auto-provisions new users with strictly CITIZEN role.
 * 4. Issues SlopeGuard application JWT (never exposes Google tokens).
 * 5. Passes JWT via short-lived HttpOnly session cookie (never in URL or query params).
 * 6. Validates redirect URI to prevent open-redirect vulnerabilities.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    public static final String OAUTH_SESSION_COOKIE_NAME = "sg_oauth_session";
    private static final int OAUTH_SESSION_EXPIRY_SECONDS = 120; // 2 minutes

    private final JwtService jwtService;
    private final UserService userService;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Value("${slopeguard.frontend.url:http://localhost:5500}")
    private String defaultFrontendUrl;

    @Value("${slopeguard.cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    public OAuth2AuthenticationSuccessHandler(
            JwtService jwtService,
            UserService userService,
            HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        String baseFrontend = resolveFrontendBaseUrl(request);

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            return UriComponentsBuilder.fromUriString(baseFrontend + "/login.html")
                    .queryParam("error", "invalid_authentication")
                    .build().toUriString();
        }

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 1. Extract verified Google OIDC attributes
        String sub;
        String email;
        Boolean emailVerified = Boolean.FALSE;
        String name;
        String picture = null;

        if (oAuth2User instanceof OidcUser oidcUser) {
            sub = oidcUser.getSubject();
            email = oidcUser.getEmail();
            emailVerified = oidcUser.getEmailVerified();
            name = oidcUser.getFullName();
            picture = oidcUser.getPicture();
        } else {
            sub = oAuth2User.getAttribute("sub");
            email = oAuth2User.getAttribute("email");
            Object ev = oAuth2User.getAttribute("email_verified");
            if (ev instanceof Boolean b) {
                emailVerified = b;
            } else if (ev instanceof String s) {
                emailVerified = Boolean.parseBoolean(s);
            }
            name = oAuth2User.getAttribute("name");
            picture = oAuth2User.getAttribute("picture");
        }

        if (sub == null || sub.isBlank()) {
            log.warn("OAuth2 Google login rejected: Missing subject identifier (sub).");
            return UriComponentsBuilder.fromUriString(baseFrontend + "/login.html")
                    .queryParam("error", "missing_subject")
                    .build().toUriString();
        }

        if (email == null || email.isBlank()) {
            log.warn("OAuth2 Google login rejected: Missing email attribute.");
            return UriComponentsBuilder.fromUriString(baseFrontend + "/login.html")
                    .queryParam("error", "missing_email")
                    .build().toUriString();
        }

        if (!Boolean.TRUE.equals(emailVerified)) {
            log.warn("OAuth2 Google login rejected: Email {} is not verified by Google.", email);
            return UriComponentsBuilder.fromUriString(baseFrontend + "/login.html")
                    .queryParam("error", "unverified_email")
                    .build().toUriString();
        }

        // 2. Resolve or provision SlopeGuard user account
        User user;
        try {
            user = userService.processOAuth2User("GOOGLE", sub, email, name, picture);
        } catch (OAuthEmailConflictException conflictEx) {
            log.warn("OAuth2 login email conflict for: {}", conflictEx.getEmail());
            return UriComponentsBuilder.fromUriString(baseFrontend + "/login.html")
                    .queryParam("error", "email_conflict")
                    .build().toUriString();
        } catch (Exception ex) {
            log.error("OAuth2 user provisioning/authentication failed for {}: {}", email, ex.getMessage());
            return UriComponentsBuilder.fromUriString(baseFrontend + "/login.html")
                    .queryParam("error", "auth_failed")
                    .build().toUriString();
        }

        // 3. Issue SlopeGuard JWT token
        String token = jwtService.generateToken(user);

        // 4. Secure delivery: Set short-lived HttpOnly session cookie (Never put JWT in redirect URL)
        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        CookieUtils.addCookie(
                response,
                OAUTH_SESSION_COOKIE_NAME,
                token,
                OAUTH_SESSION_EXPIRY_SECONDS,
                true,
                isSecure
        );

        // 5. Clean redirect to frontend login with success flag
        return UriComponentsBuilder.fromUriString(baseFrontend + "/login.html")
                .queryParam("oauth", "success")
                .build().toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    private String resolveFrontendBaseUrl(HttpServletRequest request) {
        Optional<String> redirectParam = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(jakarta.servlet.http.Cookie::getValue);

        if (redirectParam.isPresent() && !redirectParam.get().isBlank()) {
            String candidate = redirectParam.get().trim();
            if (isAllowedRedirectUri(candidate)) {
                return candidate.replaceAll("/+$", "");
            }
        }

        // Check Origin or Referer header if matching allowed origins
        String origin = request.getHeader("Origin");
        if (origin != null && isAllowedRedirectUri(origin)) {
            return origin.replaceAll("/+$", "");
        }

        return defaultFrontendUrl.replaceAll("/+$", "");
    }

    private boolean isAllowedRedirectUri(String uri) {
        try {
            URI parsed = URI.create(uri);
            String host = parsed.getHost();
            int port = parsed.getPort();
            String scheme = parsed.getScheme();
            String candidateOrigin = scheme + "://" + host + (port != -1 ? ":" + port : "");

            List<String> allowed = Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .map(s -> s.replaceAll("/+$", ""))
                    .toList();

            return allowed.contains(candidateOrigin) || allowed.contains(uri.replaceAll("/+$", ""));
        } catch (Exception e) {
            return false;
        }
    }
}

