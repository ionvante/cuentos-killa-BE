package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.JwtUtil;
import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.LoginResponse;
import com.forjix.cuentoskilla.repository.UserRepository;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authManager, JwtUtil jwtUtil, UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public User register(@Valid @RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        return userRepo.save(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse>  login(@RequestBody Map<String, String> body) {
        System.out.println("🗝️  Login attempt for email:" + body.get("email") +" - pass:"+ body.get("password") );
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(body.get("email"), body.get("password")));
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        String token = jwtUtil.generateToken(userDetails.getUsername());
         // Obtener el usuario completo desde la base de datos
         User user = userRepo.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        user.setOrders(null); // Evitar circular references en la serialización
    return ResponseEntity.ok(new LoginResponse(token, user));

        
    }
}
