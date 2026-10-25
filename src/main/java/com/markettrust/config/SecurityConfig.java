package com.markettrust.config;

import com.markettrust.security.JwtAuthenticationFilter;
import com.markettrust.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    @Value("${app.cors.allowed-origin-patterns:https://*.vercel.app,https://*.onrender.com,http://localhost:*,http://127.0.0.1:*,*}")
    private List<String> allowedOriginPatterns;

    // -------------------------------------------------------------------------
    // Public endpoints (no authentication required)
    // -------------------------------------------------------------------------
    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/products/search",
            "/api/products/nearby",
            "/api/categories",
            "/api/categories/**",
    };

    private static final String[] PUBLIC_ENDPOINTS = {
            "/",
            "/health",
            "/api/health",
            "/error",
            "/favicon.ico",
            "/api/auth/**",
            // Swagger / OpenAPI
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            // Actuator health (public)
            "/actuator/health",
            "/actuator/info",
    };

    // -------------------------------------------------------------------------
    // Security filter chain
    // -------------------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless REST — disable session and CSRF
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                    // Allow preflight CORS OPTIONS requests for ALL paths
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Public endpoints (any HTTP method)
                    .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                    // Public read-only product/category endpoints
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                    // Public seller profile
                    .requestMatchers(HttpMethod.GET, "/api/sellers/{id}/public").permitAll()
                    // Single product detail
                    .requestMatchers(HttpMethod.GET, "/api/products/{id}").permitAll()
                    // Admin-only area
                    .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                    // Everything else requires authentication
                    .anyRequest().authenticated()
            )

            // Stateless exception handling
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setContentType("application/json");
                        response.setStatus(401);
                        response.getWriter().write("""
                                {"status":401,"error":"Unauthorized","message":"Authentication required"}
                                """);
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setContentType("application/json");
                        response.setStatus(403);
                        response.getWriter().write("""
                                {"status":403,"error":"Forbidden","message":"Access denied"}
                                """);
                    })
            )

            // Register filters: rate-limit → jwt → (existing chain)
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // -------------------------------------------------------------------------
    // Authentication
    // -------------------------------------------------------------------------

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // -------------------------------------------------------------------------
    // CORS
    // -------------------------------------------------------------------------

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Support allowed origin patterns (allows wildcard matching for *.vercel.app, *.onrender.com, etc.)
        if (allowedOriginPatterns != null && !allowedOriginPatterns.isEmpty()) {
            config.setAllowedOriginPatterns(allowedOriginPatterns);
        } else {
            config.setAllowedOriginPatterns(List.of(
                    "https://*.vercel.app",
                    "https://*.onrender.com",
                    "http://localhost:*",
                    "http://127.0.0.1:*",
                    "*"
            ));
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept",
                "X-Requested-With", "Cache-Control", "Origin",
                "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        config.setExposedHeaders(List.of(
                "Authorization", "X-RateLimit-Limit", "X-RateLimit-Remaining"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
