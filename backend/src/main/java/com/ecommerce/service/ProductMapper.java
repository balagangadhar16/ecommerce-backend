package com.ecommerce.service;

import com.ecommerce.dto.ProductResponse;
import com.ecommerce.entity.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProductMapper {

    public List<ProductResponse> toResponses(List<Product> products, Map<Integer, String> imageUrlsByProductId) {
        return products.stream()
                .map(product -> toResponse(product, imageUrlsByProductId.get(product.getId())))
                .toList();
    }

    public ProductResponse toResponse(Product product, String imageUrl) {
        return ProductResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrl(imageUrl)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
