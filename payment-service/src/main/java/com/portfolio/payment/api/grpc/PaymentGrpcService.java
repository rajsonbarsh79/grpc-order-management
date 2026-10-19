package com.portfolio.payment.api.grpc;

import com.portfolio.contracts.payment.v1.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PaymentGrpcService extends PaymentServiceGrpc.PaymentServiceImplBase {

    private final ConcurrentMap<String, String> paymentsByIdempotencyKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> paymentStates = new ConcurrentHashMap<>();

    @Override
    public void authorizePayment(AuthorizePaymentRequest request,
                                 StreamObserver<AuthorizePaymentResponse> observer) {
        if (request.getOrderId().isBlank() || request.getAmountMinorUnits() <= 0
                || request.getCurrency().isBlank() || request.getIdempotencyKey().isBlank()) {
            observer.onError(Status.INVALID_ARGUMENT
                    .withDescription("order_id, positive amount, currency and idempotency_key are required")
                    .asRuntimeException());
            return;
        }

        String paymentId = paymentsByIdempotencyKey.computeIfAbsent(
                request.getIdempotencyKey(), ignored -> UUID.randomUUID().toString());
        paymentStates.putIfAbsent(paymentId, true);

        observer.onNext(AuthorizePaymentResponse.newBuilder()
                .setPaymentId(paymentId)
                .setAuthorized(true)
                .setMessage("Demo authorization accepted; no real payment was processed.")
                .build());
        observer.onCompleted();
    }

    @Override
    public void refundPayment(RefundPaymentRequest request,
                              StreamObserver<RefundPaymentResponse> observer) {
        if (request.getPaymentId().isBlank()) {
            observer.onError(Status.INVALID_ARGUMENT
                    .withDescription("payment_id is required").asRuntimeException());
            return;
        }
        boolean refunded = paymentStates.replace(request.getPaymentId(), true, false);
        observer.onNext(RefundPaymentResponse.newBuilder().setRefunded(refunded).build());
        observer.onCompleted();
    }
}
