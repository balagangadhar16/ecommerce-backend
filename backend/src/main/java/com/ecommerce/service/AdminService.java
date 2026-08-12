package com.ecommerce.service;

import com.ecommerce.dto.AdminProductRequest;
import com.ecommerce.dto.AdminUserUpdateRequest;
import com.ecommerce.dto.BusinessAnalyticsResponse;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.dto.UserResponse;
import com.ecommerce.entity.PaymentStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceAlreadyExistsException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private static final PaymentStatus SUCCESS_STATUS = PaymentStatus.PAID;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse addProduct(AdminProductRequest request) {
        categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(categoryRepository.getReferenceById(request.getCategoryId()))
                .build();

        Product saved = productRepository.save(product);

        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            productImageRepository.save(ProductImage.builder()
                    .productId(saved.getId())
                    .imageUrl(request.getImageUrl())
                    .build());
        }

        log.info("Admin added product with id: {}", saved.getId());
        return productMapper.toResponse(saved, request.getImageUrl());
    }

    @Transactional
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Order history must never be broken: products referenced by order items cannot be deleted.
        if (orderItemRepository.existsByProductId(id)) {
            throw new BadRequestException("Product is part of existing orders and cannot be deleted");
        }

        productImageRepository.deleteByProductId(id);
        cartRepository.deleteByProduct_Id(id);
        productRepository.delete(product);

        log.info("Admin deleted product with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return List.of();
        }
        return productMapper.toResponses(products, findPrimaryImagesByProduct(products));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserDetails(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Integer id, AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String normalizedEmail = request.getEmail().toLowerCase();
        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("Email is already registered");
                });

        user.setUsername(request.getUsername());
        user.setEmail(normalizedEmail);
        user.setRole(parseRole(request.getRole()));

        User saved = userRepository.save(user);
        log.info("Admin updated user with id: {}", saved.getId());
        return toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public BusinessAnalyticsResponse getDayBusiness(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long orders = orderRepository.countByPaymentStatusAndCreatedAtBetween(SUCCESS_STATUS, start, end);
        BigDecimal revenue = orderRepository.sumTotalAmountByPaymentStatusAndCreatedAtBetween(SUCCESS_STATUS, start, end);
        return buildResponse(date.toString(), orders, revenue);
    }

    @Transactional(readOnly = true)
    public BusinessAnalyticsResponse getMonthBusiness(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        long orders = orderRepository.countByPaymentStatusAndCreatedAtBetween(SUCCESS_STATUS, start, end);
        BigDecimal revenue = orderRepository.sumTotalAmountByPaymentStatusAndCreatedAtBetween(SUCCESS_STATUS, start, end);
        return buildResponse(month.toString(), orders, revenue);
    }

    @Transactional(readOnly = true)
    public BusinessAnalyticsResponse getYearBusiness(Year year) {
        LocalDateTime start = year.atDay(1).atStartOfDay();
        LocalDateTime end = start.plusYears(1);
        long orders = orderRepository.countByPaymentStatusAndCreatedAtBetween(SUCCESS_STATUS, start, end);
        BigDecimal revenue = orderRepository.sumTotalAmountByPaymentStatusAndCreatedAtBetween(SUCCESS_STATUS, start, end);
        return buildResponse(year.toString(), orders, revenue);
    }

    @Transactional(readOnly = true)
    public BusinessAnalyticsResponse getOverallBusiness() {
        long orders = orderRepository.countByPaymentStatus(SUCCESS_STATUS);
        BigDecimal revenue = orderRepository.sumTotalAmountByPaymentStatus(SUCCESS_STATUS);
        return buildResponse("Overall", orders, revenue);
    }

    private BusinessAnalyticsResponse buildResponse(String period, long orders, BigDecimal revenue) {
        return BusinessAnalyticsResponse.builder()
                .period(period)
                .totalOrders(orders)
                .totalRevenue(revenue)
                .build();
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role: " + role + ". Allowed values are CUSTOMER and ADMIN");
        }
    }

    private java.util.Map<Integer, String> findPrimaryImagesByProduct(List<Product> products) {
        List<Integer> productIds = products.stream().map(Product::getId).toList();
        return productImageRepository.findByProductIdIn(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProductImage::getProductId,
                        ProductImage::getImageUrl,
                        (first, second) -> first));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
