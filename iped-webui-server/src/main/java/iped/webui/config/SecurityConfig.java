package iped.webui.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Spring Security configuration for iped-webui-server.
 *
 * <p>Auth strategy (Phase 3):
 *
 * <ul>
 *   <li>Static assets ({@code /assets/**}, {@code /webjars/**}) and the Angular islands bundle are
 *       public — the browser fetches them before any session is established.
 *   <li>HTMX fragments and workspace pages require authentication, enforced at the filter chain
 *       level so unauthenticated HTMX responses return 401 instead of a redirect loop.
 *   <li>The {@code /api/**} reverse-proxy to Jersey carries its own API-key authentication
 *       (enforced by {@code ApiKeyAuthFilter} in iped-webapi); this chain simply passes the request
 *       through.
 *   <li>CSRF: cookie-based token (compatible with HTMX's {@code hx-headers:'{"X-CSRF-TOKEN":"…"}'}
 *       pattern).
 * </ul>
 *
 * <p>In the initial deployment, basic HTTP auth against a single admin credential configured via
 * {@code iped.webui.admin-password} is the default. OAuth2/OIDC delegated auth is deferred to Phase
 * 4.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // ── CSRF ───────────────────────────────────────────────────────
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    // /api/** proxy: Jersey enforces its own auth; exclude to avoid
                    // double-token complexity. /cases/**, /login, /logout: unauthenticated
                    // or session-teardown endpoints where CSRF adds no meaningful protection.
                    .ignoringRequestMatchers("/api/**", "/login", "/logout", "/cases", "/cases/**"))

        // ── CSP and security headers ────────────────────────────────────
        .headers(
            headers ->
                headers
                    // Content-Security-Policy tuned for the workspace:
                    //   default-src 'self'          — baseline deny-all
                    //   script-src  'self'          — island JS bundles from /islands/browser/
                    //   style-src   'self' 'unsafe-inline' — Rocker SSR inline styles
                    //   img-src     'self' data: blob: — gallery thumbnails + blob URLs
                    //   media-src   'self' blob:    — video island
                    //   frame-src   'self'          — viewer iframe for HTML renditions
                    //   object-src  'self'          — <embed> for PDF rendering
                    //   connect-src 'self'          — HTMX + island fetch calls
                    //   worker-src  blob:           — hex-viewer OffscreenCanvas worker (future)
                    //   font-src    'self'          — local web fonts
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self'; "
                                    +
                                    // unsafe-inline: Rocker SSR inline styles; googleapis: IBM Plex
                                    // font stylesheet
                                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                    + "img-src 'self' data: blob:; "
                                    + "media-src 'self' blob:; "
                                    + "frame-src 'self'; "
                                    + "object-src 'self'; "
                                    + "connect-src 'self'; "
                                    + "worker-src blob:; "
                                    +
                                    // fonts.gstatic.com: actual IBM Plex font files served by
                                    // Google
                                    "font-src 'self' https://fonts.gstatic.com; "
                                    + "base-uri 'self'; "
                                    + "form-action 'self'"))
                    // Prevent the browser from MIME-sniffing responses away from declared
                    // content-type
                    .contentTypeOptions(ct -> {})
                    // Strict-Transport-Security: enforce HTTPS for 1 year once deployed behind TLS
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                    // Suppress referer for cross-origin navigations (forensic data must not leak
                    // via Referer)
                    .referrerPolicy(
                        ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                    // Block clickjacking: the workspace must not be embedded in a foreign frame
                    .frameOptions(frame -> frame.deny())
                    // XSS auditor (legacy browsers)
                    .xssProtection(
                        xss ->
                            xss.headerValue(
                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    // Remove X-Powered-By / Server disclosure headers
                    .permissionsPolicy(
                        pp -> pp.policy("camera=(), microphone=(), geolocation=(), payment=()")))

        // ── Authorization ───────────────────────────────────────────────
        .authorizeHttpRequests(
            auth ->
                auth
                    // Static assets — browsers fetch these before any session exists
                    .requestMatchers(
                        "/islands/**", "/assets/**", "/webjars/**", "/favicon.ico", "/error")
                    .permitAll()
                    // Health / actuator probes — no auth
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    // Jersey proxy — auth delegated to ApiKeyAuthFilter in iped-webapi
                    .requestMatchers("/api/**")
                    .permitAll()
                    // Everything else requires a valid session
                    .anyRequest()
                    .authenticated())

        // ── Form login (Phase 3 baseline; OIDC deferred to Phase 4) ────
        .formLogin(
            form ->
                form.loginPage("/login")
                    .defaultSuccessUrl("/cases", false)
                    .failureUrl("/login?error")
                    .permitAll())
        // HTTP Basic for CLI/API clients (curl, scripts)
        .httpBasic(basic -> {})
        // POST /logout clears the session and returns to the login page
        .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());

    return http.build();
  }
}
