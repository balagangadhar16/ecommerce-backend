package com.ecommerce.repository;

import com.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Guards product deletion: prevents removing a product referenced by historical order items.
    boolean existsByProductId(Integer productId);
}