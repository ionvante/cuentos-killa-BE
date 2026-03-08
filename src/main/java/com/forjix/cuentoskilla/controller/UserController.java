package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.model.DTOs.UserProfileDTO;
import com.forjix.cuentoskilla.model.DTOs.UserResponseDTO;
import com.forjix.cuentoskilla.service.OrderService;
import com.forjix.cuentoskilla.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de Gestión de Usuarios
 * 
 * Rutas: /api/v1/users
 * 
 * Funcionalidad:
 * - GET /pedidos: Obtener pedidos del usuario
 * - GET /perfil: Obtener datos del perfil
 * - PUT /perfil: Actualizar perfil
 * - GET /usuarios: Listar todos los usuarios (solo ADMIN)
 * 
 * Seguridad (OWASP):
 * - Autenticación requerida
 * - Datos sensibles no expuestos (sin contraseñas)
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final OrderService orderService;
    private final UserService userService;

    public UserController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    /**
     * Extrae UserDetailsImpl del contexto de seguridad actual
     */
    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        throw new IllegalStateException("Usuario no autenticado o UserDetailsImpl no disponible");
    }

    /**
     * Obtener pedidos del usuario autenticado
     * GET /api/v1/users/pedidos
     * Acceso: Usuario autenticado
     */
    @GetMapping("/pedidos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PedidoDTO>>> getMisPedidos() {
        UserDetailsImpl user = getCurrentUser();
        logger.info("GET /api/v1/users/pedidos - Obteniendo pedidos de usuario: {}", user.getId());
        List<PedidoDTO> pedidos = orderService.getOrders(user.getId());
        return ResponseEntity.ok(ApiResponse.success(pedidos, "Pedidos obtenidos exitosamente"));
    }

    /**
     * Obtener perfil del usuario autenticado
     * GET /api/v1/users/perfil
     * Acceso: Usuario autenticado
     */
    @GetMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getPerfil() {
        UserDetailsImpl user = getCurrentUser();
        logger.info("GET /api/v1/users/perfil - Obteniendo perfil de usuario: {}", user.getId());
        return userService.findById(user.getId())
                .map(u -> ResponseEntity.ok(ApiResponse.success(UserResponseDTO.from(u), "Perfil obtenido exitosamente")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("USER_NOT_FOUND", "Usuario no encontrado")));
    }

    /**
     * Actualizar perfil del usuario autenticado
     * PUT /api/v1/users/perfil
     * Acceso: Usuario autenticado
     */
    @PutMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> updatePerfil(
            @RequestBody UserProfileDTO dto) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("PUT /api/v1/users/perfil - Usuario {} actualizando su perfil", user.getId());
        try {
            User existingUser = userService.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Actualizar solo campos permitidos
            existingUser.setNombre(dto.getNombre());
            existingUser.setApellido(dto.getApellido());
            existingUser.setTelefono(dto.getTelefono());
            existingUser.setDocumento(dto.getDocumento());
            
            User updated = userService.save(existingUser);
            logger.info("Perfil de usuario {} actualizado exitosamente", user.getId());
            
            return ResponseEntity.ok(ApiResponse.success(updated, "Perfil actualizado exitosamente"));
        } catch (Exception e) {
            logger.error("Error al actualizar perfil de usuario {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("UPDATE_ERROR", "Error al actualizar el perfil"));
        }
    }

    /**
     * Listar todos los usuarios (solo admin)
     * GET /api/v1/users
     * Acceso: Solo ADMIN
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<User>>> getUsers() {
        logger.info("GET /api/v1/users - Admin listando todos los usuarios");
        return userService.findAll()
                .map(users -> ResponseEntity.ok(ApiResponse.success(users, "Usuarios obtenidos exitosamente")))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(List.of(), "No hay usuarios")));
    }
}
