package com.edurite.security.config;

import com.edurite.security.filter.JwtAuthenticationFilter;
import com.edurite.security.service.CustomUserDetailsService;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for API authorization, authentication, password encoding, and CORS.
 */
@Configuration
public class SecurityConfig {

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            DaoAuthenticationProvider authenticationProvider,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authenticationProvider(authenticationProvider)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/api/test/**",
                                "/api/v1/institutions/**",
                                "/api/institutions/**",
                                "/api/v1/public/psychometric/**",
                                "/api/public/psychometric/**",
                                "/api/v1/public/discovery/**",
                                "/api/public/discovery/**",
                                "/api/v1/public/schools",
                                "/api/public/schools",
                                "/api/v1/subscriptions/plans",
                                "/api/subscriptions/plans",
                                "/api/v1/payments/callback",
                                "/api/payments/callback",
                                "/api/v1/payments/callbacks/**",
                                "/api/payments/callbacks/**",
                                "/api/v1/payments/webhooks/**",
                                "/api/payments/webhooks/**",
                                "/api/v1/payments/payfast/notify",
                                "/api/payments/payfast/notify",
                                "/api/v1/payments/payfast/return",
                                "/api/payments/payfast/return",
                                "/api/v1/payments/payfast/cancel",
                                "/api/payments/payfast/cancel",
                                "/api/v1/webhooks/whatsapp",
                                "/api/webhooks/whatsapp",
                                "/api/v1/ai/test",
                                "/api/ai/test",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/careers",
                                "/api/careers",
                                "/api/v1/careers/*",
                                "/api/careers/*",
                                "/api/v1/courses",
                                "/api/courses",
                                "/api/v1/courses/*",
                                "/api/courses/*",
                                "/api/v1/bursaries",
                                "/api/bursaries",
                                "/api/v1/bursaries/*",
                                "/api/bursaries/*"
                        ).permitAll()
                        // Temporary permitAll for easier integration testing of Adzuna search.
                        // Can be tightened to student auth once client-side auth flow is confirmed in QA.
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/search", "/api/jobs/search").permitAll()
                        .requestMatchers("/api/v1/student/**", "/api/student/**").hasAnyAuthority("ROLE_STUDENT", "STUDENT")
                        .requestMatchers(
                                "/api/v1/bursaries/recommendations/**",
                                "/api/bursaries/recommendations/**",
                                "/api/v1/bursaries/*/applications",
                                "/api/bursaries/*/applications",
                                "/api/v1/recommendations/**",
                                "/api/recommendations/**",
                                "/api/v1/subscriptions/**",
                                "/api/subscriptions/**",
                                "/api/v1/applications/**",
                                "/api/applications/**"
                        ).hasAnyAuthority("ROLE_STUDENT", "STUDENT")
                        .requestMatchers(
                                "/api/v1/notifications/**",
                                "/api/notifications/**"
                        ).hasAnyAuthority(
                                "ROLE_STUDENT", "STUDENT",
                                "ROLE_SCHOOL_STUDENT", "SCHOOL_STUDENT",
                                "ROLE_SCHOOL_ADMIN", "SCHOOL_ADMIN",
                                "ROLE_TEACHER", "TEACHER",
                                "ROLE_DISTRICT_ADMIN", "DISTRICT_ADMIN",
                                "ROLE_DISTRICT_DIRECTOR", "DISTRICT_DIRECTOR",
                                "ROLE_CIRCUIT_MANAGER", "CIRCUIT_MANAGER",
                                "ROLE_SUBJECT_ADVISOR", "SUBJECT_ADVISOR",
                                "ROLE_COMPANY", "COMPANY",
                                "ROLE_ADMIN", "ADMIN"
                        )
                        .requestMatchers("/api/v1/companies/**", "/api/companies/**").hasAnyAuthority("ROLE_COMPANY", "COMPANY")
                        .requestMatchers("/api/v1/admin/**", "/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers("/api/v1/district/**", "/api/district/**").hasAnyAuthority(
                                "ROLE_DISTRICT_ADMIN", "DISTRICT_ADMIN",
                                "ROLE_DISTRICT_DIRECTOR", "DISTRICT_DIRECTOR",
                                "ROLE_CIRCUIT_MANAGER", "CIRCUIT_MANAGER",
                                "ROLE_SUBJECT_ADVISOR", "SUBJECT_ADVISOR"
                        )
                        .requestMatchers("/api/v1/school/**", "/api/school/**", "/api/v1/school-admin/**", "/api/school-admin/**").hasAnyAuthority("ROLE_SCHOOL_ADMIN", "SCHOOL_ADMIN")
                        .requestMatchers("/api/v1/teacher/**", "/api/teacher/**").hasAnyAuthority("ROLE_TEACHER", "TEACHER")
                        .requestMatchers("/api/v1/school-student/**", "/api/school-student/**").hasAnyAuthority("ROLE_SCHOOL_STUDENT", "SCHOOL_STUDENT", "ROLE_STUDENT", "STUDENT")
                        .requestMatchers(
                                "/api/v1/account/**",
                                "/api/account/**",
                                "/api/v1/ai/**",
                                "/api/ai/**",
                                "/api/v1/payments/**",
                                "/api/payments/**"
                        ).authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173,http://127.0.0.1:5174,http://192.168.1.120:5173,http://192.168.1.120:5174,http://edurite.org,http://www.edurite.org,https://edurite.org,https://www.edurite.org,http://edurite.net,http://www.edurite.net,https://edurite.net,https://www.edurite.net}") String allowedOriginsCsv
    ) {
        Set<String> requiredOrigins = new LinkedHashSet<>(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174",
                "http://192.168.1.120:5173",
                "http://192.168.1.120:5174",
                "https://edurite.org",
                "https://www.edurite.org",
                "https://edurite.net",
                "https://www.edurite.net"
        ));

        Set<String> allowedOriginsSet = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        allowedOriginsSet.addAll(requiredOrigins);
        List<String> allowedOrigins = List.copyOf(allowedOriginsSet);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

