package com.iloveshopping.repository;

import com.iloveshopping.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByProviderId(String providerId);

    List<Payment> findByOrderId(String orderId);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Payment> findByProviderAndStatus(Payment.PaymentProvider provider, Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.provider = :provider AND p.status = :status AND p.createdAt < :cutoff")
    List<Payment> findStalePendingPayments(
            @Param("provider") Payment.PaymentProvider provider,
            @Param("status") Payment.PaymentStatus status,
            @Param("cutoff") java.time.LocalDateTime cutoff);

    @Query("SELECT p FROM Payment p WHERE p.provider = :provider AND p.providerId = :providerId")
    Optional<Payment> findByProviderAndProviderId(@Param("provider") Payment.PaymentProvider provider, @Param("providerId") String providerId);

    boolean existsByProviderId(String providerId);
}