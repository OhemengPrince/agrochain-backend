package com.agrochain.backend.service;

import com.agrochain.backend.dto.PaymentResponse;
import com.agrochain.backend.model.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paystackReference(payment.getPaystackReference())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .momoNumber(payment.getMomoNumber())
                .build();
    }
}
