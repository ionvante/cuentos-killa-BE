package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.PaymentVoucher;
import com.forjix.cuentoskilla.model.Rol;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.service.PaymentVoucherService;
import com.forjix.cuentoskilla.service.storage.StorageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PaymentVoucherController {

    private final PaymentVoucherService voucherService;
    private final OrderRepository orderRepository;

    public PaymentVoucherController(PaymentVoucherService voucherService, OrderRepository orderRepository) {
        this.voucherService = voucherService;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/{id}/voucher")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> upload(@PathVariable("id") Long orderId,
                                    @RequestParam("file") MultipartFile file,
                                    @AuthenticationPrincipal UserDetailsImpl user) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        if (!order.getUser().getId().equals(user.getId())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (order.getEstado() == OrderStatus.PAGADO) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error","Order already paid"));
        try {
            PaymentVoucher voucher = voucherService.upload(orderId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", voucher.getId(),
                    "filename", voucher.getFilename(),
                    "mimeType", voucher.getMimeType(),
                    "size", voucher.getSize(),
                    "firebasePath", voucher.getFirebasePath(),
                    "uploadDate", voucher.getUploadDate().toString()
            ));
        } catch (StorageException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/voucher-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUrl(@PathVariable("id") Long orderId) {
        return voucherService.findByOrder(orderId)
                .map(v -> ResponseEntity.ok(Map.of("url", voucherService.generateSignedUrl(v))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}/voucher")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> delete(@PathVariable("id") Long orderId,
                                    @AuthenticationPrincipal UserDetailsImpl user) {
        PaymentVoucher voucher = voucherService.findByOrder(orderId).orElse(null);
        if (voucher == null) return ResponseEntity.notFound().build();
        Order order = voucher.getOrder();
        if (!user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (!(order.getEstado() == OrderStatus.PAGO_PENDIENTE || order.getEstado() == OrderStatus.PAGO_ENVIADO)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
        voucherService.delete(voucher);
        return ResponseEntity.noContent().build();
    }
}
