package com.ecommerce.controller;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.CartCountResponse;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartRequest;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(request));
    }

    @GetMapping("/items")
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @GetMapping("/items/count")
    public ResponseEntity<CartCountResponse> getCartCount() {
        return ResponseEntity.ok(cartService.getCartCount());
    }

    @PutMapping("/update")
    public ResponseEntity<CartResponse> updateCart(@Valid @RequestBody UpdateCartRequest request) {
        return ResponseEntity.ok(cartService.updateQuantity(request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Integer id) {
        return ResponseEntity.ok(cartService.removeItem(id));
    }
}
