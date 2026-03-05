package com.forjix.cuentoskilla.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro para agregar headers de seguridad HTTP según OWASP
 * 
 * Headers implementados:
 * - X-Content-Type-Options: evita MIME type sniffing
 * - X-Frame-Options: evita clickjacking
 * - X-XSS-Protection: protección contra XSS
 * - Strict-Transport-Security: fuerza HTTPS
 * - Content-Security-Policy: controla recursos permitidos
 * - Cache-Control: controla caché de datos sensibles
 */
@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Prevenir MIME type sniffing
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        
        // Prevenir clickjacking
        httpResponse.setHeader("X-Frame-Options", "DENY");
        
        // Protección XSS
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Fuerza HTTPS (comentar en desarrollo)
        // httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        
        // Content Security Policy básico
        httpResponse.setHeader("Content-Security-Policy", 
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:;");
        
        // Referrer Policy
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy (antes Feature-Policy)
        httpResponse.setHeader("Permissions-Policy", 
            "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()");
        
        // Prevenir caché de datos sensibles
        httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        httpResponse.setHeader("Pragma", "no-cache");
        
        chain.doFilter(request, response);
    }
}
