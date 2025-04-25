package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}
