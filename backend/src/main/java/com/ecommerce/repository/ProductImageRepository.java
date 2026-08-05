package com.ecommerce.repository;

import com.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    // Single batched lookup so listing products does not trigger per-product image queries.
    List<ProductImage> findByProductIdIn(Collection<Integer> productIds);
}