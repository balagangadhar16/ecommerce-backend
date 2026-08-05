package com.ecommerce.service;

import com.ecommerce.dto.CategoryResponse;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(category -> CategoryResponse.builder()
                        .categoryId(category.getId())
                        .categoryName(category.getName())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        return productService.getProductsByCategory(categoryId);
    }
}
