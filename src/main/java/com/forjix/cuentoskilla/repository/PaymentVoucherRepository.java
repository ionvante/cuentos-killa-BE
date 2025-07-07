package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.PaymentVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentVoucherRepository extends JpaRepository<PaymentVoucher, Long> {
    Optional<PaymentVoucher> findByOrder_Id(Long orderId);
}
