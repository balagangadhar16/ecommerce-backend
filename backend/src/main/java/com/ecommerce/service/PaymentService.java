package com.ecommerce.service;

import com.ecommerce.config.RazorpayConfig;
import com.ecommerce.dto.CreatePaymentOrderResponse;
import com.ecommerce.dto.PaymentVerifyRequest;
import com.ecommerce.dto.PaymentVerifyResponse;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final BigDecimal PAISE_PER_UNIT = BigDecimal.valueOf(100);
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(499);
    private static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(49);

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;

    public CreatePaymentOrderResponse createOrder() {
        User user = getCurrentUser();

        BigDecimal total = computeGrandTotalInUnits();
        long amountInPaisa = toPaisa(total);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaisa);
            orderRequest.put("currency", razorpayConfig.getCurrency());
            orderRequest.put("receipt", "receipt_" + UUID.randomUUID().toString().substring(0, 8));

            Order order = razorpayClient.orders.create(orderRequest);
            log.info("Razorpay order {} created for user {}", order.get("id"), user.getEmail());

            return CreatePaymentOrderResponse.builder()
                    .orderId((String) order.get("id"))
                    .amount(amountInPaisa)
                    .currency(razorpayConfig.getCurrency())
                    .keyId(razorpayConfig.getKeyId())
                    .build();
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for user {}", user.getEmail(), e);
            throw new BadRequestException("Unable to create payment order. Please try again.");
        }
    }

    public PaymentVerifyResponse verifyPayment(PaymentVerifyRequest request) {
        boolean valid = verifySignature(request);

        if (!valid) {
            log.warn("Payment signature verification failed for order {}", request.getRazorpayOrderId());
            return PaymentVerifyResponse.builder()
                    .success(false)
                    .message("Payment verification failed. Please contact support.")
                    .build();
        }

        User user = getCurrentUser();

        // Create the Order + OrderItems, reduce stock, store payment details and clear the cart.
        var order = orderService.createOrderFromPayment(user, request.getRazorpayPaymentId());

        log.info("Payment verified and order {} created for user {} (payment {})",
                order.getOrderNumber(), user.getEmail(), request.getRazorpayPaymentId());

        return PaymentVerifyResponse.builder()
                .success(true)
                .message("Payment successful")
                .orderNumber(order.getOrderNumber())
                .paymentId(request.getRazorpayPaymentId())
                .amountPaid(order.getTotalAmount())
                .build();
    }

    private BigDecimal computeGrandTotalInUnits() {
        BigDecimal subtotal = cartService.getCart().getGrandTotal();
        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : SHIPPING_FEE;
        return subtotal.add(shipping);
    }

    private boolean verifySignature(PaymentVerifyRequest request) {
        String data = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    razorpayConfig.getKeySecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = bytesToHex(rawHmac);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    request.getRazorpaySignature().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error while verifying payment signature for order {}", request.getRazorpayOrderId(), e);
            return false;
        }
    }

    private long toPaisa(BigDecimal amountInUnits) {
        return amountInUnits.multiply(PAISE_PER_UNIT).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}