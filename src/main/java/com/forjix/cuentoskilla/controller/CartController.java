package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.CartItem;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.service.CartService;

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
 * API de Gestión del Carrito de Compras
 * 
 * Rutas: /api/v1/cart
 * 
 * Funcionalidad:
 * - GET: Obtener items del carrito (usuario autenticado)
 * - POST: Agregar item al carrito
 * - DELETE: Eliminar item del carrito
 * 
 * Seguridad (OWASP):
 * - Autenticación requerida
 * - Rate limiting aplicado
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        throw new IllegalStateException("Usuario no autenticado o UserDetailsImpl no disponible");
    }

    /**
     * Obtener items del carrito del usuario autenticado
     * GET /api/v1/cart
     * Acceso: Usuario autenticado
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CartItem>>> getCart() {
        UserDetailsImpl user = getCurrentUser();
        logger.info("GET /api/v1/cart - Obteniendo carrito de usuario: {}", user.getId());
        List<CartItem> items = service.getItems(user.getId());
        return ResponseEntity.ok(ApiResponse.success(items, "Items del carrito obtenidos exitosamente"));
    }

    /**
     * Agregar item al carrito
     * POST /api/v1/cart
     * Acceso: Usuario autenticado
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartItem>> add(
            @RequestBody CartItem item) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("POST /api/v1/cart - Usuario {} agregando item al carrito", user.getId());
        try {
            CartItem saved = service.save(item);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(saved, "Item agregado al carrito exitosamente"));
        } catch (Exception e) {
            logger.error("Error al agregar item al carrito para usuario {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("ADD_ERROR", "Error al agregar item al carrito"));
        }
    }

    /**
     * Eliminar item del carrito
     * DELETE /api/v1/cart/{id}
     * Acceso: Usuario autenticado
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable Long id) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("DELETE /api/v1/cart/{} - Usuario {} eliminando item del carrito", id, user.getId());
        try {
            service.delete(id);
            logger.info("Item {} eliminado del carrito de usuario {}", id, user.getId());
            return ResponseEntity.ok(ApiResponse.success(null, "Item eliminado del carrito exitosamente"));
        } catch (Exception e) {
            logger.error("Error al eliminar item {} del carrito: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("DELETE_ERROR", "Error al eliminar item del carrito"));
        }
    }
}
