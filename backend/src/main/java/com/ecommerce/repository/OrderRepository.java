package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByPaymentId(String paymentId);

    // Eagerly loads items + product + category to avoid N+1 when rendering order history.
    // Only returns successful (paid) orders belonging to the given user.
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    List<Order> findAllByUser_IdAndPaymentStatusOrderByCreatedAtDesc(Integer userId, PaymentStatus paymentStatus);
}