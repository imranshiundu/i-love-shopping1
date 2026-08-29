package com.iloveshopping.repository;

import com.iloveshopping.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    Optional<Order> findByNumber(String number);

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Order> findByStatus(Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatusIn(@Param("userId") String userId, @Param("statuses") List<Order.OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    long countByUserId(@Param("userId") String userId);

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.user.id = :userId AND o.status != 'CANCELLED' AND o.status != 'REFUNDED'")
    java.math.BigDecimal getTotalSpentByUser(@Param("userId") String userId);

    boolean existsByNumber(String number);

    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :cutoff")
    List<Order> findStalePendingOrders(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId "
            + "AND (:hasStatus = false OR o.status = :status) "
            + "AND (:hasFrom = false OR o.createdAt >= :from) "
            + "AND (:hasTo = false OR o.createdAt <= :to)")
    Page<Order> findByUserIdFiltered(
            @Param("userId") String userId,
            @Param("hasStatus") boolean hasStatus,
            @Param("status") Order.OrderStatus status,
            @Param("hasFrom") boolean hasFrom,
            @Param("from") LocalDateTime from,
            @Param("hasTo") boolean hasTo,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.user IS NULL AND LOWER(o.guestEmail) = LOWER(:email)")
    List<Order> findGuestOrdersByEmail(@Param("email") String email);
}