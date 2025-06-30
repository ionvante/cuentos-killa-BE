package com.forjix.cuentoskilla.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        http
        // .cors()
        // .and()
        // .csrf(csrf -> csrf.disable())
        //     .authorizeHttpRequests(auth -> auth
        //         .requestMatchers("http://localhost:4200").permitAll() // o el puerto que uses
        //         .requestMatchers("https://cuentos-killa-fe.vercel.app").permitAll() // o el puerto que uses
        //         .requestMatchers("/api/auth/**").permitAll()
        //         .requestMatchers("/api/cuentos/**").permitAll()  // 🔓 Público
        //         .requestMatchers("/api/orders/**").authenticated() // Now requires authentication
        //         .requestMatchers("/api/**").authenticated()
        //         .anyRequest().permitAll()
        //     )

         // 1) habilita CORS y deshabilita CSRF (útil para APIs REST)
          .cors(Customizer.withDefaults())
          .csrf(csrf -> csrf.disable())

          // 2) configura tus reglas de seguridad
          .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/cuentos/**").permitAll()  // 🔓 Público
            .requestMatchers("/api/orders/**").authenticated() // Now requires authentication
              .anyRequest().authenticated()
          )        
            .addFilterBefore(new JwtAuthFilter(jwtUtil, userDetailsService), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new FirebaseTokenFilter(), JwtAuthFilter.class);



        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // config.setAllowedOrigins(List.of("http://localhost:4200")); // Origen del frontend
        // config.setAllowedOrigins(List.of("https://cuentos-killa-fe.vercel.app")); // Origen del frontend
        config.setAllowedOrigins(List.of("http://localhost:4200", "https://cuentos-killa-fe.vercel.app"));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // Si usas cookies/token

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
        return new BCryptPasswordEncoder();
    }
}
