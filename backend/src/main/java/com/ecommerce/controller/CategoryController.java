package com.ecommerce.controller;

import com.ecommerce.dto.CategoryResponse;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getProductsByCategory(id));
    }
}