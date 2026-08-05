package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Eagerly loads category to avoid an N+1 query when listing products.
    @EntityGraph(attributePaths = "category")
    @Override
    List<Product> findAll();

    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryId(Integer categoryId);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Integer id);
}