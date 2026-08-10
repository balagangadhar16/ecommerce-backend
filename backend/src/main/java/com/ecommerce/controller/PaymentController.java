package com.ecommerce.controller;

import com.ecommerce.dto.CreatePaymentOrderResponse;
import com.ecommerce.dto.PaymentVerifyRequest;
import com.ecommerce.dto.PaymentVerifyResponse;
import com.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<CreatePaymentOrderResponse> createOrder() {
        return ResponseEntity.ok(paymentService.createOrder());
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentVerifyResponse> verifyPayment(@Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }
}