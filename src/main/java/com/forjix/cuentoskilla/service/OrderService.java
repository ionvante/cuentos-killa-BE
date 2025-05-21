package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderItem;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.model.DTOs.PedidoItemDTO;
import com.forjix.cuentoskilla.repository.CuentoRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepo;
    private final CuentoRepository cuentoRepo;
     private final UserRepository userRepo;


    public OrderService(OrderRepository orderRepo, CuentoRepository cuentoRepo,UserRepository userRepo) {
        this.orderRepo = orderRepo;
        this.cuentoRepo = cuentoRepo;
        this.userRepo = userRepo;
    }

    public List<Order> getOrders(User user) {
        return orderRepo.findByUser(user);
    }

     public Order getOrder(Long id) {
        return orderRepo.findById(id).orElse(null);
    }

    public Order save(PedidoDTO pedidoDTO) {
        Order order = new Order();
        order.setEstado(pedidoDTO.getEstado());
        order.setFecha(LocalDateTime.now());

        if (pedidoDTO.getUserId() != null) {
            // Usuario autenticado
            Optional<User> user = userRepo.findById(pedidoDTO.getUserId());
            user.ifPresent(order::setUser);
        } else if (pedidoDTO.getCorreoUsuario() != null) {
            // Buscar por correo
            Optional<User> user = userRepo.findByEmail(pedidoDTO.getCorreoUsuario());
            user.ifPresent(order::setUser);
        }

        // List<OrderItem> items = new ArrayList<>();

        // for (PedidoItemDTO itemDTO : pedidoDTO.getItems()) {
        //     Cuento cuento = cuentoRepo.findById(itemDTO.getCuentoId())
        //             .orElseThrow(() -> new RuntimeException("Cuento no encontrado con ID: " + itemDTO.getCuentoId()));

        //     OrderItem item = new OrderItem();
        //     item.setCuento(cuento);
        //     item.setCantidad(itemDTO.getCantidad());
        //     item.setPrecioUnitario(cuento.getPrecio());
        //     item.setOrder(order);
        //     items.add(item);
        // }

    List<OrderItem> items = pedidoDTO.getItems().stream().map(dto -> {
        OrderItem item = new OrderItem();
        item.setCantidad(dto.getCantidad());
        Cuento cuento = cuentoRepo.findById(dto.getCuentoId())
                     .orElseThrow(() -> new RuntimeException("Cuento no encontrado con ID: " + dto.getCuentoId()));        
        item.setCuento(cuento);
        item.setPrecioUnitario(cuento.getPrecio());
        item.setOrder(order); // vínculo bidireccional
        return item;
    }).collect(Collectors.toList());
        order.setItems(items);
        return orderRepo.save(order);
    }

    public void delete(Long id) {
        orderRepo.deleteById(id);
    }
}
