package com.forjix.cuentoskilla.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de Rate Limiting orientado a proteger contra abuso/bots
 * sin afectar la experiencia del usuario legítimo.
 *
 * Política:
 * - Login/Register: 30 intentos por minuto por IP (protección contra
 * brute-force)
 * - Navegación general: Sin límite práctico (1000 req/min)
 * → Un usuario real jamás alcanzará este límite
 * → Solo se bloquean scrapers/bots
 */
@Component
public class RateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Límites generosos para no afectar clientes reales
    private static final int GENERAL_LIMIT = 1000; // prácticamente sin límite
    private static final int AUTH_LIMIT = 30; // login/register: 30/min es muy generoso
    private static final long WINDOW_SIZE = 60_000; // ventana de 1 minuto

    private final ConcurrentHashMap<String, RateLimitEntry> counters = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String ip = extractIp(req);
        String path = req.getRequestURI();
        boolean isAuth = path.contains("/auth/login") || path.contains("/auth/register");

        // Solo aplicamos rate limit a rutas sensibles (auth)
        // Para el resto, el límite es tan alto que solo atrapa bots
        String key = ip + (isAuth ? ":auth" : ":general");
        int limit = isAuth ? AUTH_LIMIT : GENERAL_LIMIT;

        RateLimitEntry entry = counters.computeIfAbsent(key, k -> new RateLimitEntry());

        // Resetear ventana si expiró
        long now = System.currentTimeMillis();
        if (now - entry.windowStart > WINDOW_SIZE) {
            entry.reset(now);
        }

        int count = entry.count.incrementAndGet();

        if (count > limit) {
            logger.warn("Rate limit: IP={}, key={}, count={}", ip, key, count);
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setContentType("application/json; charset=UTF-8");
            res.setHeader("Retry-After", "60");

            String msg = isAuth
                    ? "Demasiados intentos de acceso. Espera un momento e intenta de nuevo."
                    : "Demasiadas solicitudes. Intenta de nuevo en un momento.";

            res.getWriter().write("{\"success\":false,\"message\":\"" + msg + "\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static class RateLimitEntry {
        volatile long windowStart = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);

        void reset(long now) {
            this.windowStart = now;
            this.count.set(0);
        }
    }
}
