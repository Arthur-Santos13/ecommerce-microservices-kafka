package com.ecommerce.payment.repository;

import com.ecommerce.payment.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
}
