package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.service.OrderService;
import com.forjix.cuentoskilla.service.StorageService;
import com.forjix.cuentoskilla.service.storage.StorageException; // Added
import com.forjix.cuentoskilla.model.Voucher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger; // Added
import org.slf4j.LoggerFactory; // Added

import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam; // Added
import org.springframework.web.multipart.MultipartFile; // Added
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
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class); // Added
    private final OrderService service;
    private final StorageService storageService;

    public OrderController(OrderService service, StorageService storageService) {
        this.service = service;
        this.storageService = storageService;
    }

    @GetMapping
    public List<Order> getOrders(User user) {
        return service.getOrders(user);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PedidoDTO pedidoDTO) {
        try {
            Order savedOrder = service.save(pedidoDTO);
            return ResponseEntity.ok(Map.of("id", savedOrder.getId()));
            // } catch (RuntimeException ex) {
            // return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
            // } catch (Exception ex) {
            // return ResponseEntity.internalServerError().body(Map.of("error", "Error
            // inesperado"));
            // }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body("Error al registrar pedido: " + ex.getMessage());
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
            return ResponseEntity.status(HttpStatus.SC_BAD_REQUEST) // Or INTERNAL_SERVER_ERROR depending on StorageException type
                             .body(Map.of("error", "Storage error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException for orderId {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input: " + e.getMessage()));
        } 
        catch (Exception e) {
            logger.error("Unexpected error during voucher upload for orderId {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                             .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
}
