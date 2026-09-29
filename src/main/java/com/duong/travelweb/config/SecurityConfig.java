package com.duong.travelweb.config;

import com.duong.travelweb.security.JsonAuthErrorHandler;
import com.duong.travelweb.security.JwtService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    /** Endpoint GET công khai (xem dữ liệu không cần đăng nhập). */
    private static final String[] PUBLIC_GET = {
            "/api/hotels/**",
            "/api/destinations/**",
            "/api/landmarks/**",
            "/api/countries/**",
            "/api/continents/**",
            "/api/amenities/**",
            "/api/room-types/**",
            "/api/cars/**",
            "/api/flights/**",
            "/api/airports/**",
            "/api/tours/**",
            "/api/reviews/**",
            "/api/posts/**",
            "/api/contact/info/**",
            "/api/about/**",
            "/api/payments/return/**",
            "/api/hotel-bookings/check-availability/",
            "/api/search/**",
            "/api/auth/oauth2/providers/",
            "/uploads/**",
    };

    /** Endpoint POST công khai. */
    private static final String[] PUBLIC_POST = {
            "/api/auth/register/",
            "/api/auth/login/",
            "/api/auth/logout/",
            "/api/auth/refresh-token/",
            "/api/auth/forgot-password/",
            "/api/auth/reset-password/",
            "/api/auth/verify-email/",
            "/api/auth/oauth2/exchange/",
            "/api/search/click/",
            "/api/contact/",
            "/api/payments/webhook/**",
    };

    private final JsonAuthErrorHandler jsonAuthErrorHandler;
    private final String jwtSecret;

    public SecurityConfig(JsonAuthErrorHandler jsonAuthErrorHandler,
                          @Value("${app.jwt.secret}") String jwtSecret) {
        this.jsonAuthErrorHandler = jsonAuthErrorHandler;
        this.jwtSecret = jwtSecret;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(jsonAuthErrorHandler)
                        .accessDeniedHandler(jsonAuthErrorHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jsonAuthErrorHandler)
                        .accessDeniedHandler(jsonAuthErrorHandler));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.ISSUER));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(JwtService.ROLES_CLAIM);
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    private SecretKey secretKey() {
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret phải dài tối thiểu 32 byte");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
