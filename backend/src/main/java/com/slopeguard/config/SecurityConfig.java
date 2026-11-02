package com.slopeguard.config;

import com.slopeguard.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.slopeguard.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.slopeguard.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration establishing a hybrid stateless JWT and OAuth2 OpenID Connect security filter chain.
 * <p>
 * Architectural Justification for CSRF & OAuth2 Session Management:
 * 1. REST APIs operate with stateless bearer tokens transferred via HTTP 'Authorization: Bearer <token>' headers.
 * 2. OAuth2 authorization code flow uses a cookie-based request repository (HttpCookieOAuth2AuthorizationRequestRepository)
 *    ensuring the authorization state is stored in short-lived, encrypted cookies rather than server-side HTTP sessions.
 * 3. Token delivery after OAuth2 login is mediated via a short-lived HttpOnly session cookie, avoiding token leakage in URLs.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    public SecurityConfig(
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource,
            HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository,
            OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler,
            OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsConfigurationSource = corsConfigurationSource;
        this.cookieAuthorizationRequestRepository = cookieAuthorizationRequestRepository;
        this.oauth2AuthenticationSuccessHandler = oauth2AuthenticationSuccessHandler;
        this.oauth2AuthenticationFailureHandler = oauth2AuthenticationFailureHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless REST APIs do not use browser session cookies; CSRF is therefore disabled
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public CORS preflight requests (permitted unconditionally)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public system health, discovery, and documentation endpoints
                        .requestMatchers(HttpMethod.GET, "/api", "/api/health").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // Public authentication endpoints (Ragul Module 1 + OAuth2)
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/register-rescue-team", "/api/auth/login", "/api/auth/logout", "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/google").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/oauth/session").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // Public publicapi endpoints (Ragul Module 1)
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                        // Public incident report summary listing (privacy-safe, hides citizen identity)
                        .requestMatchers(HttpMethod.GET, "/api/reports").permitAll()

                        // Authenticated citizen profile endpoint (Ragul Module 1)
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me", "/api/users/me/password").authenticated()

                        // Incident report submission and citizen-owned report access
                        .requestMatchers(HttpMethod.GET, "/api/reports/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/reports/{id}").authenticated()
                        .requestMatchers("/api/reports/{id}/photos/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reports").authenticated()

                        // Official Alert Workflow RBAC
                        .requestMatchers("/api/admin/alerts/**").hasAnyRole("ADMIN", "RESCUE_FORCE", "OFFICER")

                        // Rescue Mission Coordination endpoints
                        .requestMatchers("/api/rescue/**").hasAnyRole("ADMIN", "RESCUE_FORCE", "OFFICER")

                        // Administrative oversight and status review requires ADMIN role
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // All other endpoints require authentication by default
                        .anyRequest().authenticated()
                )
                // Configure OAuth2 Login for Google OpenID Connect
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authEndpoint -> authEndpoint
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository)
                        )
                        .redirectionEndpoint(redirectionEndpoint -> redirectionEndpoint
                                .baseUri("/login/oauth2/code/*")
                        )
                        .successHandler(oauth2AuthenticationSuccessHandler)
                        .failureHandler(oauth2AuthenticationFailureHandler)
                )
                // Configure OAuth2 Resource Server for JWT Bearer tokens
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // Centralize JSON authentication & authorization error responses
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}
