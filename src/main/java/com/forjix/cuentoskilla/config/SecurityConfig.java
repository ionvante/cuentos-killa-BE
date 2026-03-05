package com.forjix.cuentoskilla.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

/**
 * Configuración de seguridad siguiendo OWASP Top 10 para APIs
 * 
 * Implementa:
 * - AuthN/AuthZ con JWT
 * - CORS restricto
 * - CSRF disabled para APIs REST
 * - Rate limiting
 * - Headers de seguridad
 * - Validación de entrada
 * - API versionado
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 1) CORS con configuración segura
                .cors(Customizer.withDefaults())

                // 2) CSRF disabled para APIs REST (aceptable)
                .csrf(csrf -> csrf.disable())

                // 3) Reglas de autorización siguiendo principios OWASP
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos - Sin autenticación
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/cuentos/**").permitAll()
                        .requestMatchers("/api/v1/ubigeo/**").permitAll()
                        .requestMatchers("/api/v1/maestros/**").permitAll()
                        .requestMatchers("/api/v1/config/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Endpoints privados - Requieren autenticación
                        .requestMatchers("/api/v1/users/**").authenticated()
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        .requestMatchers("/api/v1/cart/**").authenticated()
                        .requestMatchers("/api/v1/direcciones/**").authenticated()

                        // Admin only
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/stats/**").hasRole("ADMIN")

                        // Webhooks - Validar con token
                        .requestMatchers("/api/v1/webhooks/**").authenticated()

                        // Cualquier otra solicitud requiere autenticación
                        .anyRequest().authenticated())

                // 4) Agregar filtros personalizados
                .addFilterBefore(new SecurityHeadersFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new RateLimitingFilter(), SecurityHeadersFilter.class)
                .addFilterBefore(new JwtAuthFilter(jwtUtil, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new FirebaseTokenFilter(), JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos (desde propiedades)
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:3000",
                "https://cuentos-killa-fe.vercel.app",
                "https://cuentos-killa-fe-ionvantes-projects.vercel.app",
                "https://killacuentos.web.app"));

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Headers permitidos (restringido, no "*")
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Accept"));

        // Headers que puede exponer el cliente
        config.setExposedHeaders(List.of(
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "Authorization"));

        // Credenciales permitidas
        config.setAllowCredentials(true);

        // Tiempo máximo de caché CORS (1 hora)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider dao = new DaoAuthenticationProvider();
        dao.setPasswordEncoder(passwordEncoder());
        dao.setUserDetailsService(userDetailsService);
        return new ProviderManager(dao);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // strength: 12 es más seguro pero más lento
    }
}
