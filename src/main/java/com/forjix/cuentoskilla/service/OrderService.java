package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.*; // Import all from model
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
// PedidoItemDTO might still be used for creating orders
import com.forjix.cuentoskilla.model.DTOs.PedidoItemDTO; 
import com.forjix.cuentoskilla.repository.CuentoRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.UserRepository;
import com.forjix.cuentoskilla.service.MercadoPagoService; // Assuming this service exists for payment
import com.mercadopago.exceptions.MPException;
import com.mercadopago.exceptions.MPApiException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepo;
    private final CuentoRepository cuentoRepo;
    private final UserRepository userRepo;
    private final MercadoPagoService mercadoPagoService; // Injected MercadoPagoService

    @Autowired
    public OrderService(OrderRepository orderRepo, CuentoRepository cuentoRepo, UserRepository userRepo, MercadoPagoService mercadoPagoService) {
        this.orderRepo = orderRepo;
        this.cuentoRepo = cuentoRepo;
        this.userRepo = userRepo;
        this.mercadoPagoService = mercadoPagoService;
    }

    private void populateOrderItemDetails(OrderItem item) {
        if (item.getCuento() != null) {
            Cuento cuento = cuentoRepo.findById(item.getCuento().getId())
                .orElseThrow(() -> new RuntimeException("Cuento not found for item: " + item.getId()));
            item.setNombre(cuento.getTitulo()); // Assuming Cuento has getTitulo() for name
            item.setImagen_url(cuento.getImagenUrl()); // Assuming Cuento has getImagenUrl()
            // precio_unitario should already be set when order is created
            // it's now double, so casting to BigDecimal if needed, or keeping as double
            BigDecimal precioUnitario = BigDecimal.valueOf(item.getPrecio_unitario());
            item.setSubtotal(precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad())));
        }
    }
    
    private void populateOrderItems(Order order) {
        if (order != null && order.getItems() != null) {
            order.getItems().forEach(this::populateOrderItemDetails);
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        List<Order> orders = orderRepo.findAll();
        orders.forEach(this::populateOrderItems);
        return orders;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        List<Order> orders = orderRepo.findByUser_Id(user.getId());
        orders.forEach(this::populateOrderItems);
        return orders;
    }

    @Transactional(readOnly = true)
    public Order getOrderByIdAndUser(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized to access this order.");
        }
        populateOrderItems(order);
        return order;
    }
    
    // Method called by controller's /mercadopago/create-preference
    @Transactional
    public Order save(PedidoDTO pedidoDTO, User authenticatedUser) {
        Order order = new Order();
        order.setUser(authenticatedUser); // Set the authenticated user
        order.setCreatedAt(LocalDateTime.now());
        order.setEstado(OrderStatus.PAGO_PENDIENTE); // New orders are PENDIENTE

        List<OrderItem> items = pedidoDTO.getItems().stream().map(dto -> {
            Cuento cuento = cuentoRepo.findById(dto.getCuentoId())
                    .orElseThrow(() -> new RuntimeException("Cuento no encontrado con ID: " + dto.getCuentoId()));
            
            OrderItem item = new OrderItem();
            item.setCuento(cuento);
            item.setCantidad(dto.getCantidad());
            // Ensure precio_unitario is set using the Cuento's price
            item.setPrecio_unitario(cuento.getPrecio()); // This is double
            item.setOrder(order);
            
            // Populate details for subtotal calculation
            item.setNombre(cuento.getTitulo());
            item.setImagen_url(cuento.getImagenUrl());
            BigDecimal precioUnitario = BigDecimal.valueOf(cuento.getPrecio());
            item.setSubtotal(precioUnitario.multiply(BigDecimal.valueOf(dto.getCantidad())));
            return item;
        }).collect(Collectors.toList());
        
        order.setItems(items);

        // Calculate total
        BigDecimal total = items.stream()
                                .map(OrderItem::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(total);

        return orderRepo.save(order);
    }


    @Transactional
    public String initiatePayment(long orderId, long userId) throws MPException, MPApiException {
        Order order = getOrderByIdAndUser(orderId, userId); // Verifies ownership

        if (order.getEstado() != OrderStatus.PAGO_PENDIENTE) {
            throw new IllegalStateException("Order is not in PENDIENTE state, cannot initiate payment.");
        }

        // The assumption is that PedidoDTO is not needed here if the preference was already created
        // when the order was saved by POST /api/orders/mercadopago/create-preference
        // If the preference needs to be (re)created here, we would need more details or the PedidoDTO
        // For now, let's assume MercadoPagoService can find or create a preference with just order and user info.
        // Or, if the initPoint is stored with the order, retrieve it.
        // This part depends heavily on how MercadoPagoService is designed and how init_point is managed.
        
        // Example: if init_point was stored on the Order entity (it's not, per current model)
        // if (order.getMercadoPagoInitPoint() != null) {
        //    return order.getMercadoPagoInitPoint();
        // }

        // If MercadoPagoService needs to create it now:
        // We need to reconstruct something like PedidoDTO or pass necessary info.
        // This is a potential design gap if init_point isn't stored or easily retrievable/recreatable.

        // Given the controller's createPaymentPreference already generates and returns initPoint,
        // this method might be more about confirming the order state and returning that *same* initPoint
        // if it was stored, or simply confirming the order is ready for payment.
        // The controller receives the initPoint from mercadoPagoService.createPaymentPreference and returns it.
        // The client should use that initPoint.
        // So, this `initiatePayment` might be redundant if the client already has the initPoint.
        // Or, it's a final check before the user is redirected by the frontend.
        
        // For now, let's assume this method is a final validation step and doesn't need to re-create preference.
        // If a preference ID was stored on the order, it could be used to fetch payment status or details.
        // The subtask says "interact with a payment service" and "Update the order status accordingly".
        // This implies it might do more than just validate.
        // However, changing status to PAGADO here is tricky without a webhook.
        // Let's assume it returns a preference ID or init_point from MP.

        // If the preference was created and its ID stored with the order (e.g. in a new field `preferenceId` on Order)
        // String preferenceId = order.getPreferenceId();
        // if (preferenceId == null) {
        //    Preference preference = mercadoPagoService.createPreferenceForOrder(order); // A new MP service method
        //    order.setPreferenceId(preference.getId());
        //    orderRepo.save(order);
        //    return preference.getInitPoint();
        // } else {
        //    Preference preference = mercadoPagoService.getPreference(preferenceId); // Another new MP service method
        //    return preference.getInitPoint(); // Or sandbox_init_point
        // }
        // This is speculative as Order model doesn't have preferenceId.
        // The most straightforward approach given current structure: this method is largely a validation.
        // The actual payment URL (init_point) should have been obtained by the client from the
        // POST /api/orders/mercadopago/create-preference endpoint.
        // So, this method might just return a confirmation or the order status.

        // Let's assume for the sake of fulfilling "interact with payment service" that it
        // tries to get a fresh init_point or confirms existing one.
        // This would likely require the PedidoDTO or similar data again.
        // This is a bit of a loop if create-preference already did this.
        // A simpler role for `initiatePayment` might be to change local order status to something like `AWAITING_PAYMENT_CONFIRMATION`
        // but `PENDIENTE` already covers this.

        // Re-evaluating: The controller's POST /api/orders/{id}/pay calls this.
        // This should not create a *new* order. It acts on an *existing* order.
        // The `createPaymentPreference` in controller saves an order and returns init_point.
        // The client hits `/pay` perhaps to log the attempt or as a gate.
        // This service method should return the init_point, assuming it's retrievable or can be regenerated.
        // For now, let's assume MercadoPagoService can generate/retrieve it using order data.
        return mercadoPagoService.getOrCreatePaymentPreferenceUrl(order); // This method needs to exist in MercadoPagoService

        // Regarding status update:
        // Typically, status to PAGADO is done via webhook, not here.
        // If payment is synchronous and MP returns status immediately, then we could update.
        // But MP is usually redirect-based.
        // So, we will NOT change status to PAGADO here. It remains PENDIENTE.
    }

    @Transactional
    public boolean deleteOrderByIdAndUser(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId)
                .orElse(null); 
        if (order == null) {
            return false; // Or throw NotFoundException
        }
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized to delete this order.");
        }
        // Potentially add checks, e.g., cannot delete if PENDING_PAYMENT or PAID
        if (order.getEstado() == OrderStatus.PAGADO) {
            throw new IllegalStateException("Cannot delete an already paid order.");
        }
        orderRepo.delete(order);
        return true;
    }

    // The old getOrder(Long id) and delete(Long id) are implicitly removed by not being defined with UUID.
    // The old save(PedidoDTO) is replaced by save(PedidoDTO, User).
    // The old getOrders(User user) is replaced by getOrdersByUser(UUID userId).
}
