package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One ordered product row inside the customer's order history.
 * Mirrors the mentor's GET /api/orders response structure: each product
 * line carries its order-level details along with the purchased product info.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistoryItemResponse {

    private Long orderId;
    private String orderNumber;
    private Integer productId;
    private String name;
    private String description;
    private String category;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal totalPrice;
    private String orderStatus;
    private LocalDateTime orderDate;
}