package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepo;

    public OrderService(OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    public List<Order> getOrders(User user) {
        return orderRepo.findByUser(user);
    }

    public Order save(Order order) {
        return orderRepo.save(order);
    }

    public void delete(Long id) {
        orderRepo.deleteById(id);
    }
}
