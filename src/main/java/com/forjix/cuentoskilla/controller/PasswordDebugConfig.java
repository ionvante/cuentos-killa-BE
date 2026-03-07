package com.forjix.cuentoskilla.controller;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordDebugConfig {
        @Bean
    CommandLineRunner printAdminPasswordHash(PasswordEncoder passwordEncoder) {
        return args -> {
            String rawPassword = "ALCuentos162902Killa";
            String hash = passwordEncoder.encode(rawPassword);
            System.out.println("HASH_ADMIN=" + hash);
        };
    }
}
