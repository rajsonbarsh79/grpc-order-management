package com.portfolio.orders.api.rest.dto;

public record OrderResponse(
        String orderId,
        String status,
        String productId,
        int quantity,
        String paymentId,
        String reservationId
) {}
