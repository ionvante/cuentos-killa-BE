package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.service.OrderService;
import com.forjix.cuentoskilla.service.MercadoPagoService; // Assuming methods will be added here
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus; // Added for consistency
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/mercadopago")
@CrossOrigin(origins = "*") // Consistent with other controllers
public class MercadoPagoWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final OrderService orderService;
    private final MercadoPagoService mercadoPagoService; // Or specific client for fetching payments

    public MercadoPagoWebhookController(OrderService orderService, MercadoPagoService mercadoPagoService) {
        this.orderService = orderService;
        this.mercadoPagoService = mercadoPagoService;
    }

    @PostMapping
    public ResponseEntity<?> handleWebhook(@RequestBody(required = false) Map<String, Object> notificationPayload, 
                                           @RequestParam Map<String, String> allRequestParams) {
        
        logger.info("Received Mercado Pago webhook. Payload: {}, Params: {}", notificationPayload, allRequestParams);

        // Handle MercadoPago's endpoint validation GET request (which has no payload)
        if (notificationPayload == null || notificationPayload.isEmpty()) {
            if (allRequestParams.containsKey("id") && allRequestParams.containsKey("topic")) { // Common for test/validation
                 logger.info("Received MP test/validation notification. ID: {}, Topic: {}", allRequestParams.get("id"), allRequestParams.get("topic"));
                 return ResponseEntity.ok().build();
            }
            // It could also be a notification type that only sends query parameters (though less common for 'payment')
            if (allRequestParams.containsKey("type") && "payment".equals(allRequestParams.get("type")) && allRequestParams.containsKey("data.id")) {
                 String paymentId = allRequestParams.get("data.id");
                 logger.info("Payment notification received via query parameters for Mercado Pago Payment ID: {}", paymentId);
                 // TODO: Fetch payment details from Mercado Pago using paymentId
                 // TODO: Update order status based on payment details
                 return ResponseEntity.ok().build();
            }
            logger.warn("Received empty or null webhook payload and not recognized as a test/validation or query param based notification.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Empty or unrecognized payload/parameters"));
        }
        
        String type = (String) notificationPayload.get("type");
        String action = (String) notificationPayload.get("action"); 
        logger.info("Notification type: {}, Action: {}", type, action);

        if ("payment".equals(type)) { 
            Object dataObject = notificationPayload.get("data");
            if (dataObject instanceof Map) {
                @SuppressWarnings("unchecked") // Safe cast after instanceof check
                Map<String, Object> data = (Map<String, Object>) dataObject;
                if (data.containsKey("id")) {
                    String paymentId = String.valueOf(data.get("id"));
                    logger.info("Payment notification received for Mercado Pago Payment ID: {}", paymentId);
                    // TODO: Fetch payment details from Mercado Pago using paymentId (in MercadoPagoService)
                    // TODO: Update order status (in OrderService) based on payment details
                    // For now, just acknowledge
                    return ResponseEntity.ok().build();
                } else {
                    logger.warn("Payment notification 'data' field is missing 'id'. Payload: {}", notificationPayload);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Missing payment ID in data"));
                }
            } else {
                 logger.warn("Payment notification 'data' field is not a Map or is missing. Payload: {}", notificationPayload);
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid 'data' field in payment notification"));
            }
        } else if ("test.created".equals(type)) { // Example of handling a specific test type if needed
             logger.info("Received MP test.created notification. Payload: {}", notificationPayload);
             return ResponseEntity.ok().build();
        }
        // Add more conditions for other types or actions as needed

        logger.info("Webhook notification type '{}' (Action: '{}') not specifically handled.", type, action);
        // Acknowledge receipt even if not fully processed to prevent MP from resending unnecessarily for unhandled types
        return ResponseEntity.ok().body(Map.of("message", "Notification received but not processed for type: " + type));
    }
}
