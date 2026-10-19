package com.portfolio.orders.api.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String productId,
        @Min(1) int quantity,
        @Min(1) long amountMinorUnits,
        @NotBlank String currency
) {}
