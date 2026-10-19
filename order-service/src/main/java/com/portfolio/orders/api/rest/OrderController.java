package com.portfolio.orders.api.rest;

import com.portfolio.orders.api.rest.dto.CreateOrderRequest;
import com.portfolio.orders.api.rest.dto.OrderResponse;
import com.portfolio.orders.infrastructure.grpc.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
        @Valid @RequestBody CreateOrderRequest request) {

        return orderService.createOrder(request);
    }
}
