package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.*;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.service.OrderService;
import com.forjix.cuentoskilla.service.StorageService;
import com.forjix.cuentoskilla.service.UserService;
import com.forjix.cuentoskilla.service.MercadoPagoService;
import com.forjix.cuentoskilla.service.PaymentVoucherService;
import com.forjix.cuentoskilla.service.storage.StorageException;
import com.mercadopago.resources.preference.Preference;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.exceptions.MPApiException;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * API de Gestión de Pedidos
 * 
 * Rutas: /api/v1/orders
 * 
 * Funcionalidad:
 * - GET: Listar pedidos
 * - GET /{id}: Obtener detalles de un pedido
 * - POST: Crear nuevo pedido
 * - POST /mercadopago/create-preference: Crear preferencia de pago
 * - PATCH /{id}/status: Actualizar estado
 * - POST /{id}/voucher: Subir comprobante
 * - DELETE /{id}: Eliminar pedido
 * 
 * Seguridad: Autenticación requerida
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final OrderService service;
    private final StorageService storageService;
    private final PaymentVoucherService voucherService;
    private final MercadoPagoService mercadoPagoService;
    private final UserService servUser;

    public OrderController(OrderService service, StorageService storageService,
            MercadoPagoService mercadoPagoService, UserService servUser,
            PaymentVoucherService voucherService) {
        this.service = service;
        this.storageService = storageService;
        this.voucherService = voucherService;
        this.mercadoPagoService = mercadoPagoService;
        this.servUser = servUser;
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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PedidoDTO>>> getOrders() {
        UserDetailsImpl user = getCurrentUser();
        logger.info("GET /api/v1/orders - Usuario: {}", user.getId());
        if (servUser.findById(user.getId()).get().getRole() == Rol.ADMIN) {
            return ResponseEntity.ok(ApiResponse.success(service.getOrders(user.getId()), "Pedidos obtenidos"));
        }
        return ResponseEntity.ok(ApiResponse.success(service.getOrdersByUser(user.getId()), "Pedidos obtenidos"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Order>> getOrderById(@PathVariable long id) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("GET /api/v1/orders/{}", id);
        Order order = service.getOrderByIdAndUser(id, user.getId());
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("ORDER_NOT_FOUND", "Pedido no encontrado"));
        }
        return ResponseEntity.ok(ApiResponse.success(order, "Pedido obtenido"));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderStatus(@PathVariable long id) {
        UserDetailsImpl user = getCurrentUser();
        try {
            OrderStatus status = service.getOrderStatus(id, user.getId());
            return ResponseEntity.ok(ApiResponse.success(Map.of("estado", status.toString()), "Estado obtenido"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("ORDER_NOT_FOUND", "Pedido no encontrado"));
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOrderStatus(
            @PathVariable long id,
            @RequestBody Map<String, String> payload) {
        UserDetailsImpl user = getCurrentUser();
        try {
            String newStatusStr = payload.get("estado");
            if (newStatusStr == null || newStatusStr.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_INPUT", "Campo 'estado' requerido"));
            }
            OrderStatus newStatus = OrderStatus.valueOf(newStatusStr);
            service.updateOrderStatus(id, newStatus, payload.get("motivo"), user.getId());
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Estado actualizado"), "Actualizado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_STATUS", "Estado inválido"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("ORDER_NOT_FOUND", "Pedido no encontrado"));
        }
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiatePayment(@PathVariable long id) {
        UserDetailsImpl user = getCurrentUser();
        try {
            String paymentResponse = service.initiatePayment(id, user.getId());
            return ResponseEntity.ok(ApiResponse.success(Map.of("paymentResponse", paymentResponse), "Pago iniciado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("PAYMENT_ERROR", "Error al procesar"));
        }
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody PedidoDTO pedidoDTO) {
        UserDetailsImpl user = getCurrentUser();
        try {
            Order savedOrder = service.save(pedidoDTO, servUser.findById(pedidoDTO.getUserId()).get());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(Map.of("id", savedOrder.getId()), "Creado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("CREATION_ERROR", "Error al crear"));
        }
    }

    @PostMapping("/mercadopago/create-preference")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentPreference(
            @RequestBody PedidoDTO pedidoDTO) {
        UserDetailsImpl user = getCurrentUser();
        Order savedOrder;
        try {
            savedOrder = service.save(pedidoDTO, servUser.findById(user.getId()).get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("ORDER_CREATION_ERROR", "Error al crear"));
        }
        try {
            Preference preference = mercadoPagoService.createPaymentPreference(pedidoDTO, savedOrder.getId());
            if (preference != null && preference.getInitPoint() != null) {
                return ResponseEntity.ok(ApiResponse.success(Map.of("initPoint", preference.getInitPoint(), "orderId", savedOrder.getId()), "Preferencia creada"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("PREFERENCE_ERROR", "Error al crear preferencia"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("MERCADO_PAGO_ERROR", "Error con Mercado Pago"));
        }
    }

    @PostMapping("/{id}/confirmar-pago-mercadopago")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmarPagoMercadoPago(@PathVariable long id) {
        UserDetailsImpl user = getCurrentUser();
        try {
            Order order = service.getOrderByIdAndUser(id, user.getId());
            if (order.getEstado() == OrderStatus.PAGADO || order.getEstado() == OrderStatus.VERIFICADO) {
                return ResponseEntity.ok(ApiResponse.success(Map.of("status", "success"), "Confirmado"));
            }
            return ResponseEntity.ok(ApiResponse.success(Map.of("status", "pending"), "Pendiente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("ORDER_NOT_FOUND", "No encontrado"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable long id) {
        UserDetailsImpl user = getCurrentUser();
        try {
            boolean deleted = service.deleteOrderByIdAndUser(id, user.getId());
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success(null, "Eliminado"));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("ACCESS_DENIED", "Sin permiso"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("DELETION_ERROR", "Error"));
        }
    }

    @PostMapping("/{id}/voucher")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadVoucher(
            @PathVariable("id") long orderId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "dispositivo", required = false, defaultValue = "Desconocido") String dispositivo,
            HttpServletRequest request) {
        UserDetailsImpl user = getCurrentUser();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("EMPTY_FILE", "Archivo vacío"));
        }
        try {
            Order order = service.getOrderByIdAndUser(orderId, user.getId());
            if (order.getEstado() == OrderStatus.PAGADO || order.getEstado() == OrderStatus.VERIFICADO) {
                return ResponseEntity.badRequest().body(ApiResponse.error("ORDER_ALREADY_PAID", "Pedido ya pagado"));
            }
            Voucher voucher = storageService.store(file, orderId, file.getOriginalFilename(), file.getContentType(), request.getRemoteAddr(), dispositivo, file.getSize());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(Map.of("id", voucher.getId(), "fileUrl", voucher.getFilePath()), "Subido"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("STORAGE_ERROR", "Error"));
        }
    }

    @PostMapping("/{id}/voucherF")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadVoucherFirebase(
            @PathVariable("id") Long orderId,
            @RequestParam("file") MultipartFile file) {
        UserDetailsImpl user = getCurrentUser();
        try {
            Order order = service.getOrderByIdAndUser(orderId, user.getId());
            if (order.getEstado() == OrderStatus.PAGADO) {
                return ResponseEntity.badRequest().body(ApiResponse.error("ORDER_ALREADY_PAID", "Pedido ya pagado"));
            }
            PaymentVoucher voucher = voucherService.upload(orderId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(Map.of("id", voucher.getId(), "filename", voucher.getFilename()), "Subido"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("INTERNAL_ERROR", "Error"));
        }
    }
}
