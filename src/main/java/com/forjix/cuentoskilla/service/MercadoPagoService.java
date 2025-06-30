package com.forjix.cuentoskilla.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.*;
import com.mercadopago.client.payment.PaymentClient; // Added
import com.mercadopago.exceptions.MPException;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.preference.Preference;
import com.mercadopago.resources.payment.Payment; // Added
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.model.DTOs.PedidoItemDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;

@Service
public class MercadoPagoService {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoService.class);

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.back-urls.success}")
    private String successUrl;

    @Value("${mercadopago.back-urls.failure}")
    private String failureUrl;

    @Value("${mercadopago.back-urls.pending}")
    private String pendingUrl;

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    // Constructor injection can be used as well, but field injection is shown in the example
    // public MercadoPagoService(@Value("${mercadopago.access-token}") String accessToken) {
    //     this.accessToken = accessToken;
    // }

    @PostConstruct
    public void initMercadoPago() {
        MercadoPagoConfig.setAccessToken(accessToken);
        logger.info("MercadoPago SDK initialized with provided access token.");
    }

    // Methods for payment preference creation will be added later

    public Preference createPaymentPreference(PedidoDTO pedidoDTO, Long orderId) throws MPException, MPApiException {
        List<PreferenceItemRequest> items = new ArrayList<>();
        for (PedidoItemDTO itemDTO : pedidoDTO.getItems()) {
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                .id(itemDTO.getCuentoId().toString())
                .title(itemDTO.getNombreCuento() != null ? itemDTO.getNombreCuento() : "Cuento")
                .quantity(itemDTO.getCantidad())
                .unitPrice(itemDTO.getPrecioUnitario())
                .build();
            items.add(itemRequest);
        }

        PreferencePayerRequest payerRequest = PreferencePayerRequest.builder()
            .email(pedidoDTO.getCorreoUsuario())
            // .name(pedidoDTO.getNombreUsuario()) // Add if available in PedidoDTO
            // .surname(pedidoDTO.getApellidoUsuario()) // Add if available in PedidoDTO
            .build();

        PreferenceBackUrlsRequest backUrlsRequest = PreferenceBackUrlsRequest.builder()
            .success(this.successUrl)
            .failure(this.failureUrl)
            .pending(this.pendingUrl)
            .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
            .items(items)
            .payer(payerRequest)
            .backUrls(backUrlsRequest)
            .notificationUrl(this.notificationUrl)
            .externalReference(String.valueOf(orderId))
            .autoReturn("approved")
            .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        logger.info("Mercado Pago preference created with ID: {}", preference.getId());
        return preference;
    }

    public Payment getPaymentDetails(Long paymentId) throws MPException, MPApiException {
        if (paymentId == null || paymentId <= 0) {
            logger.warn("Invalid paymentId provided to getPaymentDetails: {}", paymentId);
            // Consider throwing a more specific custom exception or using MPException if appropriate
            throw new IllegalArgumentException("Payment ID must be a positive Long value."); 
        }
        logger.info("Fetching payment details for Mercado Pago Payment ID: {}", paymentId);
        
        PaymentClient client = new PaymentClient();
        Payment payment = client.get(paymentId);

        if (payment != null) {
            // Log more details if available and relevant, e.g., status, external_reference
            logger.info("Successfully fetched payment details for ID {}. Status: {}, Order External Ref: {}", 
                        payment.getId(), payment.getStatus(), payment.getExternalReference());
        } else {
            // This case might not be reachable if client.get() throws an exception for not found
            // However, if it can return null (e.g. due to an API change or SDK behavior), this is a good check.
            logger.warn("No payment details found for Mercado Pago Payment ID: {}. The SDK might have returned null.", paymentId);
            // Depending on requirements, you might throw an exception here if a payment not being found is an error.
            // For example: throw new MPException("Payment with ID " + paymentId + " not found.");
        }
        return payment;
    }
}
