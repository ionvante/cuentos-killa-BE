package com.forjix.cuentoskilla;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CuentosKillaApplication {
    public static void main(String[] args) {
        SpringApplication.run(CuentosKillaApplication.class, args);
    }
}