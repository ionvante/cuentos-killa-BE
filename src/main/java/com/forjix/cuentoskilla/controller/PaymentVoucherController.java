package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.PaymentVoucher;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.service.PaymentVoucherService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * API de Gestión de Comprobantes de Pago
 * 
 * Rutas: /api/v1/pedidos
 * 
 * Funcionalidad:
 * - GET /{id}/voucher-url: Obtener URL firmada del comprobante
 * - DELETE /{id}/voucher: Eliminar comprobante
 * 
 * Seguridad (OWASP):
 * - Validación de propiedad de datos
 * - URLs firmadas con tiempo de expiración
 */
@RestController
@RequestMapping("/api/v1/pedidos")
public class PaymentVoucherController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentVoucherController.class);
    private final PaymentVoucherService voucherService;
    private final OrderRepository orderRepository;

    public PaymentVoucherController(PaymentVoucherService voucherService, OrderRepository orderRepository) {
        this.voucherService = voucherService;
        this.orderRepository = orderRepository;
    }

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        throw new IllegalStateException("Usuario no autenticado o UserDetailsImpl no disponible");
    }

    /**
     * Obtener URL firmada del comprobante de pago
     * GET /api/v1/pedidos/{id}/voucher-url
     * Acceso: Solo ADMIN
     */
    @GetMapping("/{id}/voucher-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUrl(@PathVariable("id") Long orderId) {
        logger.info("GET /api/v1/pedidos/{}/voucher-url - Admin obteniendo URL del comprobante", orderId);
        Optional<PaymentVoucher> voucher = voucherService.findByOrder(orderId);
        if (voucher.isPresent()) {
            String url = voucherService.generateSignedUrl(voucher.get());
            return ResponseEntity.ok(ApiResponse.success(
                Map.of("url", url),
                "URL firmada del comprobante obtenida exitosamente"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("VOUCHER_NOT_FOUND", "Comprobante no encontrado para el pedido"));
        }
    }

    /**
     * Eliminar comprobante de pago
     * DELETE /api/v1/pedidos/{id}/voucher
     * Acceso: ADMIN o propietario del pedido
     */
    @DeleteMapping("/{id}/voucher")
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") Long orderId) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("DELETE /api/v1/pedidos/{}/voucher - Usuario {} eliminando comprobante", orderId, user.getId());
        
        Optional<PaymentVoucher> voucherOpt = voucherService.findByOrder(orderId);
        if (voucherOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("VOUCHER_NOT_FOUND", "Comprobante no encontrado"));
        }
        
        PaymentVoucher voucher = voucherOpt.get();
        Order order = voucher.getOrder();
        
        // Verificar permisos: ADMIN o propietario del pedido
        boolean isAdmin = user.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin && !order.getUser().getId().equals(user.getId())) {
            logger.warn("Acceso denegado al intento de eliminar comprobante del pedido {} por usuario {}", orderId, user.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("ACCESS_DENIED", "No tienes permiso para eliminar este comprobante"));
        }
        
        // Validar estado del pedido si el usuario no es ADMIN
        if (!isAdmin) {
            if (order.getEstado() != OrderStatus.PAGO_PENDIENTE && order.getEstado() != OrderStatus.PAGO_ENVIADO) {
                logger.warn("Intento de eliminar comprobante en estado no permitido {} para pedido {}", order.getEstado(), orderId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_ORDER_STATE", "No se puede eliminar el comprobante en este estado del pedido"));
            }
        }
        
        voucherService.delete(voucher);
        logger.info("Comprobante eliminado exitosamente para pedido {}", orderId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Comprobante eliminado exitosamente"));
    }
}
