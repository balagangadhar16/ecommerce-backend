package com.ecommerce.repository;

import com.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartItem, Integer> {

    Optional<CartItem> findByUser_IdAndProduct_Id(Integer userId, Integer productId);

    // Eagerly loads product + category to avoid N+1 when rendering the cart.
    @EntityGraph(attributePaths = {"product", "product.category"})
    List<CartItem> findAllByUser_Id(Integer userId);

    // Total number of units in the cart (used for the navbar badge).
    @Query("select coalesce(sum(c.quantity), 0) from CartItem c where c.user.id = :userId")
    Integer sumQuantityByUser_Id(@Param("userId") Integer userId);

    // Removes all cart entries referencing a product before the product itself is deleted.
    void deleteByProduct_Id(Integer productId);
}
