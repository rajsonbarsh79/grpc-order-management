package com.portfolio.orders.infrastructure.grpc;

import com.portfolio.contracts.inventory.v1.CheckAvailabilityResponse;
import com.portfolio.contracts.inventory.v1.ReserveStockResponse;
import com.portfolio.contracts.payment.v1.AuthorizePaymentResponse;
import com.portfolio.orders.api.rest.dto.CreateOrderRequest;
import com.portfolio.orders.api.rest.dto.OrderResponse;
import com.portfolio.orders.infrastructure.grpc.OrderDownstreamClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderDownstreamClient downstreamClient;

    public OrderService(OrderDownstreamClient downstreamClient) {
        this.downstreamClient = downstreamClient;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {

        String orderId = UUID.randomUUID().toString();

        // 1. Check inventory availability
        CheckAvailabilityResponse availability =
            downstreamClient.checkAvailability(
                request.productId(),
                request.quantity()
            );

        if (!availability.getAvailable()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Insufficient inventory"
            );
        }

        // 2. Reserve stock
        ReserveStockResponse reservation =
            downstreamClient.reserveStock(
                orderId,
                request.productId(),
                request.quantity(),
                orderId + ":reserve"
            );

        if (!reservation.getReserved()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Inventory reservation failed"
            );
        }

        // 3. Authorize payment
        AuthorizePaymentResponse authorization;

        try {
            authorization = downstreamClient.authorizePayment(
                orderId,
                request.amountMinorUnits(),
                request.currency(),
                orderId + ":payment"
            );
        } catch (RuntimeException paymentFailure) {
            releaseReservationSafely(
                reservation.getReservationId(),
                orderId
            );

            // Preserve the original payment failure.
            throw paymentFailure;
        }

        // 4. Handle a declined payment
        if (!authorization.getAuthorized()) {

            releaseReservationSafely(
                reservation.getReservationId(),
                orderId
            );

            throw new ResponseStatusException(
                HttpStatus.PAYMENT_REQUIRED,
                "Payment was not authorized"
            );
        }

        // 5. Return confirmed order
        return new OrderResponse(
            orderId,
            "CONFIRMED",
            request.productId(),
            request.quantity(),
            authorization.getPaymentId(),
            reservation.getReservationId()
        );
    }

    private void releaseReservationSafely(
        String reservationId,
        String orderId) {

        try {
            downstreamClient.releaseReservation(
                reservationId,
                orderId,
                orderId + ":release"
            );
        } catch (RuntimeException releaseFailure) {
            log.error(
                "Failed to release inventory reservation for orderId={}. "
                    + "Reconciliation is required.",
                orderId,
                releaseFailure
            );
        }
    }
}
