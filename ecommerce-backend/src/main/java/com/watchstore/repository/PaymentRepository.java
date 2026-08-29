package com.watchstore.repository;

import com.watchstore.model.Payment;
import com.watchstore.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionUuid(String transactionUuid);
    List<Payment> findByOrderId(Long orderId);
    // used by the reconciliation job to find stuck attempts
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime cutoff);
}
