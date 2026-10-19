
package com.portfolio.orders.infrastructure.grpc;

import com.portfolio.contracts.inventory.v1.CheckAvailabilityRequest;
import com.portfolio.contracts.inventory.v1.CheckAvailabilityResponse;
import com.portfolio.contracts.inventory.v1.InventoryServiceGrpc;
import com.portfolio.contracts.inventory.v1.ReleaseReservationRequest;
import com.portfolio.contracts.inventory.v1.ReleaseReservationResponse;
import com.portfolio.contracts.inventory.v1.ReserveStockRequest;
import com.portfolio.contracts.inventory.v1.ReserveStockResponse;
import com.portfolio.contracts.payment.v1.AuthorizePaymentRequest;
import com.portfolio.contracts.payment.v1.AuthorizePaymentResponse;
import com.portfolio.contracts.payment.v1.PaymentServiceGrpc;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@Service
public class OrderDownstreamClient {

    private static final Logger log = LoggerFactory.getLogger(OrderDownstreamClient.class);

    private final InventoryServiceGrpc.InventoryServiceBlockingStub inventory;
    private final PaymentServiceGrpc.PaymentServiceBlockingStub payment;

    public OrderDownstreamClient(
        InventoryServiceGrpc.InventoryServiceBlockingStub inventory,
        PaymentServiceGrpc.PaymentServiceBlockingStub payment) {
        this.inventory = inventory;
        this.payment = payment;
    }

    @Retry(name = "inventoryRead")
    @CircuitBreaker(name = "inventory", fallbackMethod = "inventoryReadFallback")
    @Bulkhead(name = "inventory")
    public CheckAvailabilityResponse checkAvailability(
        String productId, int quantity) {

        try {
            return inventory
                .withDeadlineAfter(2, TimeUnit.SECONDS)
                .checkAvailability(
                    CheckAvailabilityRequest.newBuilder()
                        .setProductId(productId)
                        .setQuantity(quantity)
                        .build()
                );
        } catch (StatusRuntimeException ex) {
            if (isTransient(ex)) {
                throw new TransientGrpcException(
                    "Transient inventory read failure", ex);
            }
            throw ex;
        }
    }

    public CheckAvailabilityResponse inventoryReadFallback(
        String productId, int quantity, Throwable ex) {
        log.error("Inventory availability check failed for productId={}",
            productId, ex);
        throw serviceUnavailable("Inventory service is temporarily unavailable", ex);
    }

    @CircuitBreaker(name = "inventory", fallbackMethod = "reserveStockFallback")
    @Bulkhead(name = "inventory")
    public ReserveStockResponse reserveStock(
        String orderId,
        String productId,
        int quantity,
        String idempotencyKey) {

        return inventory
            .withDeadlineAfter(2, TimeUnit.SECONDS)
            .reserveStock(
                ReserveStockRequest.newBuilder()
                    .setOrderId(orderId)
                    .setProductId(productId)
                    .setQuantity(quantity)
                    .setIdempotencyKey(idempotencyKey)
                    .build()
            );
    }

    public ReserveStockResponse reserveStockFallback(
        String orderId,
        String productId,
        int quantity,
        String idempotencyKey,
        Throwable ex) {
        log.error("Inventory reservation failed for orderId={}", orderId, ex);
        throw serviceUnavailable("Inventory service is temporarily unavailable", ex);
    }

    @CircuitBreaker(name = "payment", fallbackMethod = "authorizePaymentFallback")
    @Bulkhead(name = "payment")
    public AuthorizePaymentResponse authorizePayment(
        String orderId,
        long amountMinorUnits,
        String currency,
        String idempotencyKey) {

        return payment
            .withDeadlineAfter(2, TimeUnit.SECONDS)
            .authorizePayment(
                AuthorizePaymentRequest.newBuilder()
                    .setOrderId(orderId)
                    .setAmountMinorUnits(amountMinorUnits)
                    .setCurrency(currency)
                    .setIdempotencyKey(idempotencyKey)
                    .build()
            );
    }

    public AuthorizePaymentResponse authorizePaymentFallback(
        String orderId,
        long amountMinorUnits,
        String currency,
        String idempotencyKey,
        Throwable ex) {
        log.error("Payment authorization failed for orderId={}", orderId, ex);
        throw serviceUnavailable("Payment service is temporarily unavailable", ex);
    }

    /*
     * Compensation call: deliberately not protected by the same inventory
     * circuit breaker, so an open breaker does not prevent a release attempt.
     * A durable retry/reconciliation mechanism is still needed for production.
     */
    public ReleaseReservationResponse releaseReservation(
        String reservationId,
        String orderId,
        String idempotencyKey) {

        return inventory
            .withDeadlineAfter(2, TimeUnit.SECONDS)
            .releaseReservation(
                ReleaseReservationRequest.newBuilder()
                    .setReservationId(reservationId)
                    .setOrderId(orderId)
                    .setIdempotencyKey(idempotencyKey)
                    .build()
            );
    }

    private boolean isTransient(StatusRuntimeException ex) {
        Status.Code code = ex.getStatus().getCode();
        return code == Status.Code.UNAVAILABLE
            || code == Status.Code.DEADLINE_EXCEEDED;
    }

    private ResponseStatusException serviceUnavailable(
        String message, Throwable cause) {
        return new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }

    public static class TransientGrpcException extends RuntimeException {
        public TransientGrpcException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
