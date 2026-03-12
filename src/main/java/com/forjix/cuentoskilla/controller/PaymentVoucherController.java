package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.PaymentVoucher;
import com.forjix.cuentoskilla.model.Voucher;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.VoucherRepository;
import com.forjix.cuentoskilla.service.PaymentVoucherService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * API de Gestión de Comprobantes de Pago
 * 
 * Rutas: /api/v1/pedidos y /api/v1/orders
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
@RequestMapping({"/api/v1/pedidos", "/api/v1/orders"})
public class PaymentVoucherController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentVoucherController.class);
    private final PaymentVoucherService voucherService;
    private final OrderRepository orderRepository;
    private final VoucherRepository localVoucherRepository;

    @Value("${storage.provider:local}")
    private String storageProvider;

    public PaymentVoucherController(PaymentVoucherService voucherService, OrderRepository orderRepository,
            VoucherRepository localVoucherRepository) {
        this.voucherService = voucherService;
        this.orderRepository = orderRepository;
        this.localVoucherRepository = localVoucherRepository;
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
     * GET /api/v1/orders/{id}/voucher-url
     * Acceso: Solo ADMIN
     */
    @GetMapping("/{id}/voucher-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUrl(@PathVariable("id") Long orderId) {
        logger.info("GET voucher-url para pedido {} - Admin obteniendo URL del comprobante", orderId);

        if (isFirebaseProvider()) {
            Optional<PaymentVoucher> voucher = voucherService.findByOrder(orderId);
            if (voucher.isPresent()) {
                try {
                    String firebasePath = voucher.get().getFirebasePath();
                    if (StringUtils.hasText(firebasePath)) {
                        String url = voucherService.generateSignedUrl(voucher.get());
                        if (StringUtils.hasText(url)) {
                            return ResponseEntity.ok(ApiResponse.success(
                                    Map.of("url", url),
                                    "URL firmada del comprobante obtenida exitosamente"));
                        }
                        logger.warn("No se pudo generar URL firmada para pedido {} (url vacia)", orderId);
                    } else {
                        logger.warn("Voucher Firebase sin firebasePath para pedido {}", orderId);
                    }
                } catch (Exception ex) {
                    logger.error("Error generando URL firmada para pedido {}: {}", orderId, ex.getMessage(), ex);
                }
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(ApiResponse.error("VOUCHER_URL_UNAVAILABLE",
                                "El comprobante existe, pero no se pudo generar su URL"));
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("VOUCHER_NOT_FOUND", "Comprobante no encontrado para el pedido"));
        }

        // Entorno local: buscar en almacenamiento local
        Optional<Voucher> localVoucher = localVoucherRepository.findFirstByOrder_IdOrderByIdDesc(orderId);
        if (localVoucher.isPresent() && StringUtils.hasText(localVoucher.get().getNombreArchivo())) {
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String url = baseUrl + "/uploads/" + localVoucher.get().getNombreArchivo();
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("url", url),
                    "URL local del comprobante obtenida exitosamente"));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("VOUCHER_NOT_FOUND", "Comprobante no encontrado para el pedido"));
    }

    /**
     * Eliminar comprobante de pago
     * DELETE /api/v1/pedidos/{id}/voucher
     * DELETE /api/v1/orders/{id}/voucher
     * Acceso: ADMIN o propietario del pedido
     */
    @DeleteMapping("/{id}/voucher")
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") Long orderId) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("DELETE voucher para pedido {} - Usuario {} eliminando comprobante", orderId, user.getId());

        Order order;
        PaymentVoucher voucher = null;
        Voucher localVoucher = null;

        if (isFirebaseProvider()) {
            Optional<PaymentVoucher> voucherOpt = voucherService.findByOrder(orderId);
            if (voucherOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("VOUCHER_NOT_FOUND", "Comprobante no encontrado"));
            }
            voucher = voucherOpt.get();
            order = voucher.getOrder();
        } else {
            Optional<Voucher> localOpt = localVoucherRepository.findFirstByOrder_IdOrderByIdDesc(orderId);
            if (localOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("VOUCHER_NOT_FOUND", "Comprobante no encontrado"));
            }
            localVoucher = localOpt.get();
            order = localVoucher.getOrder();
        }

        // Verificar permisos: ADMIN o propietario del pedido
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !order.getUser().getId().equals(user.getId())) {
            logger.warn("Acceso denegado al intento de eliminar comprobante del pedido {} por usuario {}", orderId,
                    user.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "No tienes permiso para eliminar este comprobante"));
        }

        // Validar estado del pedido si el usuario no es ADMIN
        if (!isAdmin) {
            if (order.getEstado() != OrderStatus.PAGO_PENDIENTE && order.getEstado() != OrderStatus.PAGO_ENVIADO) {
                logger.warn("Intento de eliminar comprobante en estado no permitido {} para pedido {}",
                        order.getEstado(), orderId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("INVALID_ORDER_STATE",
                                "No se puede eliminar el comprobante en este estado del pedido"));
            }
        }

        if (isFirebaseProvider()) {
            voucherService.delete(voucher);
        } else {
            if (StringUtils.hasText(localVoucher.getFilePath())) {
                try {
                    Files.deleteIfExists(Path.of(localVoucher.getFilePath()));
                } catch (Exception ex) {
                    logger.warn("No se pudo eliminar archivo local de comprobante para pedido {}: {}",
                            orderId, ex.getMessage());
                }
            }
            localVoucherRepository.delete(localVoucher);
        }
        logger.info("Comprobante eliminado exitosamente para pedido {}", orderId);

        return ResponseEntity.ok(ApiResponse.success(null, "Comprobante eliminado exitosamente"));
    }

    private boolean isFirebaseProvider() {
        return storageProvider != null && storageProvider.equalsIgnoreCase("firebase");
    }
}
