package com.ecommerce.service;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.CartCountResponse;
import com.ecommerce.dto.CartItemResponse;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartRequest;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        User user = getCurrentUser();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        CartItem existing = cartRepository.findByUser_IdAndProduct_Id(user.getId(), product.getId()).orElse(null);

        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + request.getQuantity();
        validateStock(product, newQuantity);

        if (existing == null) {
            CartItem item = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartRepository.save(item);
            log.info("Added product {} to cart for user {}", product.getId(), user.getEmail());
        } else {
            existing.setQuantity(newQuantity);
            cartRepository.save(existing);
            log.info("Updated quantity for product {} in cart for user {}", product.getId(), user.getEmail());
        }

        return getCart(user);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart() {
        return getCart(getCurrentUser());
    }

    @Transactional(readOnly = true)
    public CartCountResponse getCartCount() {
        User user = getCurrentUser();
        Integer count = cartRepository.sumQuantityByUser_Id(user.getId());
        return CartCountResponse.builder().count(count == null ? 0 : count).build();
    }

    @Transactional
    public CartResponse updateQuantity(UpdateCartRequest request) {
        User user = getCurrentUser();

        CartItem item = findOwnedItem(user, request.getId());
        validateStock(item.getProduct(), request.getQuantity());

        item.setQuantity(request.getQuantity());
        cartRepository.save(item);
        log.info("Cart item {} quantity updated to {} for user {}", item.getId(), request.getQuantity(), user.getEmail());

        return getCart(user);
    }

    @Transactional
    public CartResponse removeItem(Integer cartItemId) {
        User user = getCurrentUser();

        CartItem item = findOwnedItem(user, cartItemId);
        cartRepository.delete(item);
        log.info("Removed cart item {} for user {}", cartItemId, user.getEmail());

        return getCart(user);
    }

    private CartItem findOwnedItem(User user, Integer cartItemId) {
        CartItem item = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Cart item not found with id: " + cartItemId);
        }
        return item;
    }

    private void validateStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new BadRequestException("Only " + product.getStock() + " units of " + product.getName() + " are available in stock");
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CartResponse getCart(User user) {
        List<CartItem> items = cartRepository.findAllByUser_Id(user.getId());
        if (items.isEmpty()) {
            return CartResponse.builder()
                    .items(List.of())
                    .totalProducts(0)
                    .totalQuantity(0)
                    .grandTotal(BigDecimal.ZERO)
                    .build();
        }

        Map<Integer, String> imageUrls = findPrimaryImages(items);
        BigDecimal grandTotal = BigDecimal.ZERO;
        List<CartItemResponse> responses = new ArrayList<>(items.size());

        for (CartItem item : items) {
            Product product = item.getProduct();
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            grandTotal = grandTotal.add(itemTotal);

            responses.add(CartItemResponse.builder()
                    .cartItemId(item.getId())
                    .productId(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .category(product.getCategory() != null ? product.getCategory().getName() : null)
                    .price(product.getPrice())
                    .imageUrl(imageUrls.get(product.getId()))
                    .quantity(item.getQuantity())
                    .totalPrice(itemTotal)
                    .build());
        }

        int totalQuantity = responses.stream().mapToInt(CartItemResponse::getQuantity).sum();

        return CartResponse.builder()
                .items(responses)
                .totalProducts(responses.size())
                .totalQuantity(totalQuantity)
                .grandTotal(grandTotal)
                .build();
    }

    private Map<Integer, String> findPrimaryImages(List<CartItem> items) {
        List<Integer> productIds = items.stream().map(item -> item.getProduct().getId()).toList();
        return productImageRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(
                        ProductImage::getProductId,
                        ProductImage::getImageUrl,
                        (first, second) -> first));
    }
}
