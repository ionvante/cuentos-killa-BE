package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderItem;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.model.DTOs.PedidoItemDTO;
import com.forjix.cuentoskilla.repository.CuentoRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepo;
    private final CuentoRepository cuentoRepo;


    public OrderService(OrderRepository orderRepo, CuentoRepository cuentoRepo) {
        this.orderRepo = orderRepo;
        this.cuentoRepo = cuentoRepo;
    }

    public List<Order> getOrders(User user) {
        return orderRepo.findByUser(user);
    }

    public Order save(PedidoDTO pedidoDTO) {
        Order order = new Order();
        order.setEstado(pedidoDTO.getEstado());
        order.setFecha(LocalDateTime.now());

        List<OrderItem> items = new ArrayList<>();

        for (PedidoItemDTO itemDTO : pedidoDTO.getItems()) {
            Cuento cuento = cuentoRepo.findById(itemDTO.getCuentoId())
                    .orElseThrow(() -> new RuntimeException("Cuento no encontrado con ID: " + itemDTO.getCuentoId()));

            OrderItem item = new OrderItem();
            item.setCuento(cuento);
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecioUnitario(cuento.getPrecio());
            item.setOrder(order);
            items.add(item);
        }

        order.setItems(items);
        return orderRepo.save(order);
    }

    public void delete(Long id) {
        orderRepo.deleteById(id);
    }
}
