package com.ecommerce.service;

import com.ecommerce.dto.ProductResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return toResponses(products);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product, findPrimaryImage(product.getId()));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Integer categoryId) {
        return toResponses(productRepository.findByCategoryId(categoryId));
    }

    private List<ProductResponse> toResponses(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrls = findPrimaryImagesByProduct(products);
        return productMapper.toResponses(products, imageUrls);
    }

    private Map<Integer, String> findPrimaryImagesByProduct(List<Product> products) {
        List<Integer> productIds = products.stream().map(Product::getId).toList();
        return productImageRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(
                        ProductImage::getProductId,
                        ProductImage::getImageUrl,
                        (first, second) -> first));
    }

    private String findPrimaryImage(Integer productId) {
        return productImageRepository.findByProductIdIn(List.of(productId)).stream()
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }
}
