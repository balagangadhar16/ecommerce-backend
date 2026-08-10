package com.ecommerce.service;

import com.ecommerce.dto.OrderHistoryItemResponse;
import com.ecommerce.dto.OrderHistoryResponse;
import com.ecommerce.dto.OrderItemResponse;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.PaymentStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(499);
    private static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(49);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;

    /**
     * Builds an Order from the user's current cart once a payment succeeds.
     * Creates order + items, reduces stock, persists payment details and clears the cart.
     */
    @Transactional
    public OrderResponse createOrderFromPayment(User user, String paymentId) {
        // Idempotency: if this payment already produced an order, return it unchanged.
        var existing = orderRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        List<CartItem> cartItems = cartRepository.findAllByUser_Id(user.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty. Cannot create an order.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .paymentId(paymentId)
                .paymentStatus(PaymentStatus.PAID)
                .orderStatus(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.ZERO)
                .build();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build();
            order.getItems().add(orderItem);

            // Reduce stock (snapshot price from the product at order time).
            int remainingStock = product.getStock() - cartItem.getQuantity();
            if (remainingStock < 0) {
                throw new BadRequestException("Only " + product.getStock() + " units of " + product.getName() + " are available in stock");
            }
            product.setStock(remainingStock);
            productRepository.save(product);
        }

        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : SHIPPING_FEE;
        order.setTotalAmount(subtotal.add(shipping));
        Order saved = orderRepository.save(order);

        // Clear the user's cart.
        cartRepository.deleteAll(cartItems);
        log.info("Order {} created for user {} with payment {}", saved.getOrderNumber(), user.getEmail(), paymentId);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderHistoryResponse getMyOrders() {
        User user = getCurrentUser();
        List<Order> orders = orderRepository
                .findAllByUser_IdAndPaymentStatusOrderByCreatedAtDesc(user.getId(), PaymentStatus.PAID);
        return toHistoryResponse(user, orders);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        User user = getCurrentUser();
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order not found with number: " + orderNumber);
        }
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        Map<Integer, String> imageUrls = findImagesForOrder(order);
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .orderItemId(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .imageUrl(imageUrls.get(item.getProduct().getId()))
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .paymentId(order.getPaymentId())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    /**
     * Builds the GET /api/orders response: a flattened list of every product
     * bought across the user's successful orders, each carrying order-level
     * details (order id/number, status, date) alongside product info.
     */
    private OrderHistoryResponse toHistoryResponse(User user, List<Order> orders) {
        Map<Integer, String> imageUrls = findImagesForOrders(orders);

        List<OrderHistoryItemResponse> products = orders.stream()
                .flatMap(order -> order.getItems().stream()
                        .map(item -> toHistoryItem(order, item, imageUrls.get(item.getProduct().getId()))))
                .toList();

        return OrderHistoryResponse.builder()
                .role(user.getRole().name())
                .orders(OrderHistoryResponse.OrderHistoryOrders.builder()
                        .products(products)
                        .build())
                .username(user.getUsername())
                .build();
    }

    private OrderHistoryItemResponse toHistoryItem(Order order, OrderItem item, String imageUrl) {
        Product product = item.getProduct();
        return OrderHistoryItemResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrl(imageUrl)
                .quantity(item.getQuantity())
                .pricePerUnit(item.getPrice())
                .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .orderStatus(order.getOrderStatus().name())
                .orderDate(order.getCreatedAt())
                .build();
    }

    private Map<Integer, String> findImagesForOrders(List<Order> orders) {
        List<Integer> productIds = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(item -> item.getProduct().getId())
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productImageRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(
                        ProductImage::getProductId,
                        ProductImage::getImageUrl,
                        (first, second) -> first));
    }

    private Map<Integer, String> findImagesForOrder(Order order) {
        List<Integer> productIds = order.getItems().stream().map(item -> item.getProduct().getId()).toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productImageRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(
                        ProductImage::getProductId,
                        ProductImage::getImageUrl,
                        (first, second) -> first));
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "ORD-" + date + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}