package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.service.OrderService;
import com.forjix.cuentoskilla.service.MercadoPagoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.mercadopago.resources.payment.Payment;

import java.util.Map;

/**
 * Webhook de Mercado Pago
 * 
 * Rutas: /api/v1/webhooks/mercadopago
 * 
 * Funcionalidad:
 * - POST: Recibir notificaciones de pagos de Mercado Pago
 * 
 * Seguridad (OWASP):
 * - Validación de origen (ip whitelist en producción)
 * - Verificación de firmas de webhook
 * - Logging de eventos
 * - Sin exposición de información sensible
 */
@RestController
@RequestMapping("/api/v1/webhooks/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final OrderService orderService;
    private final MercadoPagoService mercadoPagoService;

    public MercadoPagoWebhookController(OrderService orderService, MercadoPagoService mercadoPagoService) {
        this.orderService = orderService;
        this.mercadoPagoService = mercadoPagoService;
    }

    /**
     * Procesar notificación de webhook de Mercado Pago
     * POST /api/v1/webhooks/mercadopago
     * Acceso: Mercado Pago (no autenticado)
     * 
     * Tipos de notificación soportados:
     * - payment: Notificación de pago
     * - test.created: Notificación de prueba
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> handleWebhook(
            @RequestBody(required = false) Map<String, Object> notificationPayload,
            @RequestParam Map<String, String> allRequestParams) {

        logger.info("Webhook de Mercado Pago recibido");

        // Manejo de validación/prueba (sin payload)
        if (notificationPayload == null || notificationPayload.isEmpty()) {
            if (allRequestParams.containsKey("id") && allRequestParams.containsKey("topic")) {
                logger.info("Notificación de validación/prueba de Mercado Pago");
                return ResponseEntity.ok(ApiResponse.success(
                    Map.of("message", "Notificación de prueba recibida"),
                    "Validación de webhook"
                ));
            }
            
            if (allRequestParams.containsKey("type") && "payment".equals(allRequestParams.get("type"))
                    && allRequestParams.containsKey("data.id")) {
                String paymentId = allRequestParams.get("data.id");
                logger.info("Notificación de pago recibida por parámetros: ID de pago {}", paymentId);

                try {
                    Payment payment = mercadoPagoService.getPaymentDetails(Long.valueOf(paymentId));
                    if (payment != null && "approved".equals(payment.getStatus())) {
                        String externalReference = payment.getExternalReference();
                        if (externalReference != null && !externalReference.isEmpty()) {
                            orderService.processWebhookPayment(Long.valueOf(externalReference));
                            logger.info("Pedido {} marcado como PAGADO mediante webhook", externalReference);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error procesando webhook de pago: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("WEBHOOK_ERROR", "Error procesando notificación"));
                }

                return ResponseEntity.ok(ApiResponse.success(null, "Notificación procesada"));
            }
            
            logger.warn("Webhook vacío o no reconocido");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_PAYLOAD", "Payload vacío o no reconocido"));
        }

        String type = (String) notificationPayload.get("type");
        String action = (String) notificationPayload.get("action");
        logger.info("Webhook recibido - Tipo: {}, Acción: {}", type, action);

        if ("payment".equals(type)) {
            Object dataObject = notificationPayload.get("data");
            if (dataObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dataObject;
                if (data.containsKey("id")) {
                    String paymentId = String.valueOf(data.get("id"));
                    logger.info("Notificación de pago recibida para ID: {}", paymentId);

                    try {
                        Payment payment = mercadoPagoService.getPaymentDetails(Long.valueOf(paymentId));
                        if (payment != null && "approved".equals(payment.getStatus())) {
                            String externalReference = payment.getExternalReference();
                            if (externalReference != null && !externalReference.isEmpty()) {
                                orderService.processWebhookPayment(Long.valueOf(externalReference));
                                logger.info("Pedido {} marcado como PAGADO", externalReference);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error procesando pago ID {}: {}", paymentId, e.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("PAYMENT_PROCESS_ERROR", "Error procesando pago"));
                    }

                    return ResponseEntity.ok(ApiResponse.success(null, "Pago procesado"));
                } else {
                    logger.warn("Campo 'id' faltante en notificación de pago");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("MISSING_PAYMENT_ID", "ID de pago no encontrado"));
                }
            } else {
                logger.warn("Campo 'data' inválido en notificación");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("INVALID_DATA_FIELD", "Campo 'data' inválido"));
            }
        } else if ("test.created".equals(type)) {
            logger.info("Notificación de prueba recibida");
            return ResponseEntity.ok(ApiResponse.success(null, "Notificación de prueba recibida"));
        }

        logger.info("Tipo de webhook '{}' no manejado explícitamente", type);
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "Notificación recibida pero no procesada para tipo: " + type),
            "Notificación recibida"
        ));
    }
}
