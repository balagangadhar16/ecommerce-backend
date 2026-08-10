package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Integer cartItemId;
    private Integer productId;
    private String name;
    private String description;
    private Integer categoryId;
    private String category;
    private BigDecimal price;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal totalPrice;
}
