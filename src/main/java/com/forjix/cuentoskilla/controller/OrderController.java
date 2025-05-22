package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.service.OrderService;
import com.forjix.cuentoskilla.service.StorageService;
import com.forjix.cuentoskilla.service.MercadoPagoService; // Added
import com.forjix.cuentoskilla.service.storage.StorageException;
import com.forjix.cuentoskilla.model.Voucher;
import com.mercadopago.resources.preference.Preference; // Added
import com.mercadopago.exceptions.MPException; // Added
import com.mercadopago.exceptions.MPApiException; // Added

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import org.apache.http.HttpStatus; // Replaced by Spring's HttpStatus
import org.springframework.http.HttpStatus; // Added
import org.springframework.http.ResponseEntity;
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
@CrossOrigin(origins = "*")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final OrderService service;
    private final StorageService storageService;
    private final MercadoPagoService mercadoPagoService; // Added

    public OrderController(OrderService service, StorageService storageService, MercadoPagoService mercadoPagoService) { // Modified
        this.service = service;
        this.storageService = storageService;
        this.mercadoPagoService = mercadoPagoService; // Added
    }

    @GetMapping
    public List<Order> getOrders(User user) {
        return service.getOrders(user);
    }

     @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return service.getOrder(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PedidoDTO pedidoDTO) {
        try {
            Order savedOrder = service.save(pedidoDTO);
            return ResponseEntity.ok(Map.of("id", savedOrder.getId()));
        } catch (Exception ex) {
            // Using Spring's HttpStatus here
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al registrar pedido: " + ex.getMessage()));
        }
    }

    @PostMapping("/mercadopago/create-preference") // New Endpoint
    public ResponseEntity<?> createPaymentPreference(@RequestBody PedidoDTO pedidoDTO) {
        Order savedOrder;
        try {
            logger.info("Saving order before creating Mercado Pago preference for user: {}", pedidoDTO.getCorreoUsuario());
            savedOrder = service.save(pedidoDTO);
            logger.info("Order saved with ID: {}. Attempting to create Mercado Pago preference.", savedOrder.getId());
        } catch (Exception e) {
            logger.error("Error saving order before creating Mercado Pago preference: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error saving order: " + e.getMessage()));
        }

        try {
            Preference preference = mercadoPagoService.createPaymentPreference(pedidoDTO, savedOrder.getId());
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
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/voucher")
    public ResponseEntity<?> uploadVoucher(
            @RequestParam("file") MultipartFile file,
            @RequestParam("idpedido") Long orderId,
            @RequestParam(name = "dispositivo", required = false, defaultValue = "Desconocido") String dispositivo,
            HttpServletRequest request) {
        
        if (file.isEmpty()) {
            logger.warn("Upload attempt with empty file for orderId: {}", orderId);
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot upload an empty file."));
        }

        try {
            String ip = request.getRemoteAddr();
            String originalFileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            long fileSize = file.getSize();

            // Basic validation for orderId before calling service (optional, as service also checks)
            if (orderId == null || orderId <= 0) {
                logger.warn("Invalid orderId provided: {}", orderId);
                return ResponseEntity.badRequest().body(Map.of("error", "Valid Order ID is required."));
            }

            logger.info("Attempting to upload voucher for orderId: {}, fileName: {}, ip: {}, device: {}", 
                        orderId, originalFileName, ip, dispositivo);

            Voucher voucher = storageService.store(file, orderId, originalFileName, contentType, ip, dispositivo, fileSize);
            
            logger.info("Voucher uploaded successfully for orderId: {}. Voucher ID: {}, FileName: {}", 
                        orderId, voucher.getId(), voucher.getNombreArchivo());
            
            return ResponseEntity.ok(Map.of(
                "message", "Voucher uploaded successfully!", 
                "voucherId", voucher.getId(),
                "fileName", voucher.getNombreArchivo()
            ));
        } catch (StorageException e) {
            logger.error("StorageException for orderId {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST) // Using Spring's HttpStatus
                             .body(Map.of("error", "Storage error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException for orderId {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input: " + e.getMessage()));
        } 
        catch (Exception e) {
            logger.error("Unexpected error during voucher upload for orderId {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR) // Using Spring's HttpStatus
                             .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
}
