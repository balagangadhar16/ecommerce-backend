package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByPaymentId(String paymentId);

    // Eagerly loads items + product + category to avoid N+1 when rendering order history.
    // Only returns successful (paid) orders belonging to the given user.
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    List<Order> findAllByUser_IdAndPaymentStatusOrderByCreatedAtDesc(Integer userId, PaymentStatus paymentStatus);

    // ----- Admin business analytics (only successful/paid orders) -----

    long countByPaymentStatusAndCreatedAtBetween(PaymentStatus status, LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.paymentStatus = :status and o.createdAt >= :start and o.createdAt < :end")
    BigDecimal sumTotalAmountByPaymentStatusAndCreatedAtBetween(@Param("status") PaymentStatus status,
                                                                @Param("start") LocalDateTime start,
                                                                @Param("end") LocalDateTime end);

    long countByPaymentStatus(PaymentStatus status);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.paymentStatus = :status")
    BigDecimal sumTotalAmountByPaymentStatus(@Param("status") PaymentStatus status);
}