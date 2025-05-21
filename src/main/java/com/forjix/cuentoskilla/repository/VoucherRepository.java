package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Optional, but good practice

@Repository // Optional for Spring Data JPA, but can be explicit
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    // You can add custom query methods here if needed in the future
}
