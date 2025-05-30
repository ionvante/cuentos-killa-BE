package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Assuming User entity has a field "id" of type UUID
    // Spring Data JPA will generate the query for "SELECT o FROM Order o WHERE o.user.id = :userId"
    List<Order> findByUser_Id(Long userId);
}
