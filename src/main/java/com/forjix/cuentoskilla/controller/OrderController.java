package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.*; // Import all from model
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.service.OrderService;
import com.forjix.cuentoskilla.service.StorageService;
import com.forjix.cuentoskilla.service.UserService;
import com.forjix.cuentoskilla.service.MercadoPagoService; // Added
import com.forjix.cuentoskilla.service.storage.StorageException;
import com.mercadopago.resources.preference.Preference; // Added
import com.mercadopago.exceptions.MPException; // Added
import com.mercadopago.exceptions.MPApiException; // Added

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import org.apache.http.HttpStatus; // Replaced by Spring's HttpStatus
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Added
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final OrderService service;
    private final StorageService storageService;
    private final MercadoPagoService mercadoPagoService; // Added
    private final UserService servUser; // Added

    public OrderController(OrderService service, StorageService storageService, MercadoPagoService mercadoPagoService,UserService servUser) { // Modified
        this.service = service;
        this.storageService = storageService;
        this.mercadoPagoService = mercadoPagoService; // Added
        this.servUser = servUser; // Added
    }


    @GetMapping
    public ResponseEntity<List<Order>> getOrder(@AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (servUser.findById(user.getId()).get().getRole().equals(Rol.ADMIN.toString())){
            return ResponseEntity.ok(service.getOrders(user.getId()));
        }
        return ResponseEntity.ok(service.getOrdersByUser(user.getId()));
    }
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable long id, @AuthenticationPrincipal UserDetailsImpl user) {        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Order order = service.getOrderByIdAndUser(id, user.getId());
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        System.out.println("getOrder getOrderByIdAndUser() ejecutado");
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> initiatePayment(@PathVariable long id, @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            // Assuming OrderService.initiatePayment checks ownership and handles payment logic
            // This might return a payment URL or status
            String paymentResponse = service.initiatePayment(id, user.getId());
            return ResponseEntity.ok(Map.of("paymentResponse", paymentResponse));
        } catch (MPApiException e) {
            logger.error("MPApiException while initiating payment for order ID {}: {} - Response: {}", id, e.getMessage(), e.getApiResponse() != null ? e.getApiResponse().getContent() : "N/A", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error with Mercado Pago API: " + e.getMessage()));
        } catch (MPException e) {
            logger.error("MPException while initiating payment for order ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error with Mercado Pago SDK: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error initiating payment for order {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error initiating payment: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PedidoDTO pedidoDTO) {
        try {
            
            Order savedOrder = service.save(pedidoDTO,servUser.findById(pedidoDTO.getUserId()).get());
            logger.info("Order saved with ID: {}", savedOrder.getId());
            return ResponseEntity.ok(Map.of("id", savedOrder.getId()));
        } catch (Exception ex) {
            // Using Spring's HttpStatus here
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al registrar pedido: " + ex.getMessage()));
        }
    }

    @PostMapping("/mercadopago/create-preference") // New Endpoint
    public ResponseEntity<?> createPaymentPreference(@RequestBody PedidoDTO pedidoDTO, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Ensure the PedidoDTO's user matches the authenticated user, or set it.
        // This depends on how PedidoDTO is designed and if it contains user info.
        // For now, assuming service.save() handles user association correctly or PedidoDTO is already user-aware.
        // If pedidoDTO.getCorreoUsuario() is used, ensure it matches user.getEmail() or similar.

        Order savedOrder;
        try {
            logger.info("Saving order before creating Mercado Pago preference for user: {}", user.getEmail());
            // Pass the authenticated user object or its ID to the service.save method
            savedOrder = service.save(pedidoDTO, user); // Modified to pass user
            logger.info("Order saved with ID: {}. Attempting to create Mercado Pago preference.", savedOrder.getId());
        } catch (Exception e) {
            logger.error("Error saving order before creating Mercado Pago preference for user {}: {}", user.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error saving order: " + e.getMessage()));
        }

        try {
            // Pass user details if needed by mercadoPagoService, or rely on savedOrder's user association
            Preference preference = mercadoPagoService.createPaymentPreference(pedidoDTO, savedOrder.getId()); // Potentially pass user
            if (preference != null && preference.getInitPoint() != null) {
                logger.info("Mercado Pago preference created successfully for order ID: {}. Init point: {}", savedOrder.getId(), preference.getInitPoint());
                return ResponseEntity.ok(Map.of("initPoint", preference.getInitPoint(), "orderId", savedOrder.getId()));
            } else {
                logger.error("Mercado Pago preference or init point was null for order ID: {}", savedOrder.getId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                     .body(Map.of("error", "Failed to retrieve Mercado Pago init point."));
            }
        } catch (MPApiException e) {
            logger.error("MPApiException while creating Mercado Pago preference for order ID {}: {} - Response: {}", savedOrder.getId(), e.getMessage(), e.getApiResponse() != null ? e.getApiResponse().getContent() : "N/A", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error with Mercado Pago API: " + e.getMessage()));
        } catch (MPException e) {
            logger.error("MPException while creating Mercado Pago preference for order ID {}: {}", savedOrder.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error with Mercado Pago SDK: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating Mercado Pago preference for order ID {}: {}", savedOrder.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Unexpected error creating preference: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Add user check in service.delete(id, userId)
        boolean deleted = service.deleteOrderByIdAndUser(id, user.getId());
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            // Could be not found or not authorized
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); 
        }
    }

    @PostMapping("/voucher")
    public ResponseEntity<?> uploadVoucher(
            @RequestParam("file") MultipartFile file,
            @RequestParam("idpedido") long orderId, // Changed to UUID
            @RequestParam(name = "dispositivo", required = false, defaultValue = "Desconocido") String dispositivo,
            HttpServletRequest request, @AuthenticationPrincipal User user) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (file.isEmpty()) {
            logger.warn("Upload attempt with empty file for orderId: {} by user {}", orderId, user.getId());
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot upload an empty file."));
        }

        try {
            String ip = request.getRemoteAddr();
            String originalFileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            long fileSize = file.getSize();

            // Basic validation for orderId before calling service
            if (orderId == 0) { // UUID can be null
                logger.warn("Null orderId provided by user {}", user.getId());
                return ResponseEntity.badRequest().body(Map.of("error", "Valid Order ID is required."));
            }

            logger.info("Attempting to upload voucher for orderId: {}, fileName: {}, ip: {}, device: {} by user {}", 
                        orderId, originalFileName, ip, dispositivo, user.getId());
            
            // storageService.store should also verify user ownership of the orderId
            Voucher voucher = storageService.store(file, orderId, originalFileName, contentType, ip, dispositivo, fileSize);
            
            logger.info("Voucher uploaded successfully for orderId: {}. Voucher ID: {}, FileName: {} by user {}", 
                        orderId, voucher.getId(), voucher.getNombreArchivo(), user.getId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Voucher uploaded successfully!", 
                "voucherId", voucher.getId(),
                "fileName", voucher.getNombreArchivo()
            ));
        } catch (StorageException e) {
            logger.error("StorageException for orderId {} by user {}: {}", orderId, user.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(Map.of("error", "Storage error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException for orderId {} by user {}: {}", orderId, user.getId(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input: " + e.getMessage()));
        } 
        catch (Exception e) { // Catch more specific security/ownership exceptions if defined
            logger.error("Unexpected error during voucher upload for orderId {} by user {}: {}", orderId, user.getId(), e.getMessage(), e);
            if (e.getMessage().toLowerCase().contains("not authorized") || e.getMessage().toLowerCase().contains("access denied")) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN)
                             .body(Map.of("error", "Access Denied: " + e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
}
