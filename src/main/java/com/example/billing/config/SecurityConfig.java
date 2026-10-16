package com.example.billing.config;

import com.example.billing.security.JsonSecurityErrorHandler;
import com.example.billing.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity            // enables @PreAuthorize on controller/service methods
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final JsonSecurityErrorHandler securityErrorHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless REST API using bearer tokens: no cookies, so CSRF protection isn't needed
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_URLS).permitAll()

                // Destructive operations: admins only
                .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/invoices/*/cancel").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Day-to-day billing work: admins and accountants
                .requestMatchers("/api/customers/**", "/api/invoices/**", "/api/payments/**")
                    .hasAnyRole("ADMIN", "ACCOUNTANT")

                .anyRequest().authenticated()
            )

            // Return JSON 401/403 instead of the default HTML/login redirect
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler)
            )

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Boot builds the DaoAuthenticationProvider automatically from the
     * UserDetailsService and PasswordEncoder beans, so we only expose the manager
     * for use in AuthController.login().
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
