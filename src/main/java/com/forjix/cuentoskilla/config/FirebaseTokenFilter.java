package com.forjix.cuentoskilla.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.forjix.cuentoskilla.model.Rol;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("FirebaseTokenFilter ejecutado");
        if (authHeader != null && authHeader.startsWith("Bearer-fb")) {
            String idToken = authHeader.substring(10);
            try {
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
                String uid = decodedToken.getUid();
                String role = decodedToken.getClaims().getOrDefault("role", "USER").toString();

                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
               

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(uid, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                System.out.println("Token Firebase inválido: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
