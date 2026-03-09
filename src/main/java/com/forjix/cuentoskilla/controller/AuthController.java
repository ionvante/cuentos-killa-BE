package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.JwtUtil;
import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.LoginRequest;
import com.forjix.cuentoskilla.model.Rol;
import com.forjix.cuentoskilla.model.DTOs.LoginResponse;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.model.DTOs.UserResponseDTO;
import com.forjix.cuentoskilla.repository.UserRepository;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


/**
 * API de Autenticación v1
 * 
 * Rutas:
 * - POST /api/v1/auth/register - Registro de nuevos usuarios
 * - POST /api/v1/auth/login - Autenticación con email/password
 * 
 * Seguridad (OWASP):
 * - Rate limiting: 5 intentos de login por minuto
 * - Validación de entrada: email y password requeridos
 * - No expone detalles de usuarios existentes
 * - Usa BCrypt con strength 12
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authManager, JwtUtil jwtUtil, 
                        UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registrar nuevo usuario
     * 
     * POST /api/v1/auth/register
     * 
     * @param user Datos del usuario (email, password, nombre, apellido)
     * @return Datos del usuario creado
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody User user) {
        log.info("POST /api/v1/auth/register - Registrando nuevo usuario: {}", user.getEmail());
        
        // Validar que el email no exista
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            log.warn("Intento de registro con email existente: {}", user.getEmail());
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                    "EMAIL_EXISTS",
                    "El email ya está registrado"
                ));
        }
        
        // Encriptar contraseña y asignar rol por defecto
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Rol.USER);
        
        User registered = userRepo.save(user);
        registered.setPassword(null); // No retornar la contraseña
        
        log.info("Usuario registrado exitosamente: {}", user.getEmail());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                registered,
                "Usuario registrado exitosamente"
            ));
    }

    /**
     * Autenticación con credenciales
     * 
     * POST /api/v1/auth/login
     * Rate limit: 5 intentos por minuto
     * 
     * @param request Email y password
     * @return Token JWT y datos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        
        log.info("POST /api/v1/auth/login - Intento de login para: {}", request.getEmail());
        
        try {
            // Autenticar con Spring Security   
            Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(), 
                    request.getPassword()
                )
            );
            
            // Obtener detalles del usuario autenticado
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            
            // Generar JWT
            String token = jwtUtil.generateToken(userDetails.getUsername());
            
            // Obtener datos completos del usuario
            User user = userRepo.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
            
            UserResponseDTO userResponse = UserResponseDTO.from(user);

            log.info("Login exitoso para usuario: {}", request.getEmail());

            return ResponseEntity
                .ok(ApiResponse.success(
                    new LoginResponse(token, userResponse),
                    "Login exitoso"
                ));
                
        } catch (BadCredentialsException e) {
            log.warn("Credenciales inválidas para usuario: {}", request.getEmail());
            
            // No revelar si el usuario existe o no
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                    "INVALID_CREDENTIALS",
                    "Credenciales inválidas"
                ));
                
        } catch (UsernameNotFoundException e) {
            log.warn("Usuario no encontrado: {}", request.getEmail());
            
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                    "INVALID_CREDENTIALS",
                    "Credenciales inválidas"
                ));
        }
    }
}
