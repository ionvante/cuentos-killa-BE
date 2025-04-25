package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
