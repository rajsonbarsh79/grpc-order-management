package com.portfolio.inventory.api.grpc;

import com.portfolio.contracts.inventory.v1.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    // Demo data only. Replace with a database-backed inventory repository in the persistence phase.
    private final Map<String, AtomicInteger> stock = new ConcurrentHashMap<>();
    private final Map<String, String> reservationsByKey = new ConcurrentHashMap<>();
    private final Map<String, String> reservationKeys = new ConcurrentHashMap<>();

    public InventoryGrpcService() {
        stock.put("SKU-100", new AtomicInteger(100));
        stock.put("SKU-200", new AtomicInteger(50));
    }

    @Override
    public void checkAvailability(CheckAvailabilityRequest request,
                                  StreamObserver<CheckAvailabilityResponse> observer) {
        if (request.getProductId().isBlank() || request.getQuantity() <= 0) {
            observer.onError(Status.INVALID_ARGUMENT
                    .withDescription("product_id and positive quantity are required").asRuntimeException());
            return;
        }

        int available = stock.computeIfAbsent(request.getProductId(), ignored -> new AtomicInteger(0)).get();
        observer.onNext(CheckAvailabilityResponse.newBuilder()
                .setProductId(request.getProductId())
                .setAvailableQuantity(available)
                .setAvailable(available >= request.getQuantity())
                .build());
        observer.onCompleted();
    }

    @Override
    public void reserveStock(ReserveStockRequest request,
                             StreamObserver<ReserveStockResponse> observer) {
        if (request.getOrderId().isBlank() || request.getProductId().isBlank()
                || request.getQuantity() <= 0 || request.getIdempotencyKey().isBlank()) {
            observer.onError(Status.INVALID_ARGUMENT
                    .withDescription("order_id, product_id, positive quantity and idempotency_key are required")
                    .asRuntimeException());
            return;
        }

        String priorReservation = reservationsByKey.get(request.getIdempotencyKey());
        if (priorReservation != null) {
            observer.onNext(ReserveStockResponse.newBuilder()
                    .setReservationId(priorReservation).setReserved(true).build());
            observer.onCompleted();
            return;
        }

        AtomicInteger quantity = stock.computeIfAbsent(request.getProductId(), ignored -> new AtomicInteger(0));
        while (true) {
            int current = quantity.get();
            if (current < request.getQuantity()) {
                observer.onNext(ReserveStockResponse.newBuilder().setReserved(false).build());
                observer.onCompleted();
                return;
            }
            if (quantity.compareAndSet(current, current - request.getQuantity())) {
                String reservationId = UUID.randomUUID().toString();
                reservationsByKey.putIfAbsent(request.getIdempotencyKey(), reservationId);
                reservationKeys.put(reservationId, request.getProductId() + ":" + request.getQuantity());
                observer.onNext(ReserveStockResponse.newBuilder()
                        .setReservationId(reservationsByKey.get(request.getIdempotencyKey()))
                        .setReserved(true).build());
                observer.onCompleted();
                return;
            }
        }
    }

    @Override
    public void releaseReservation(ReleaseReservationRequest request,
                                  StreamObserver<ReleaseReservationResponse> observer) {
        String reservation = reservationKeys.remove(request.getReservationId());
        if (reservation != null) {
            String[] parts = reservation.split(":");
            stock.computeIfAbsent(parts[0], ignored -> new AtomicInteger(0))
                    .addAndGet(Integer.parseInt(parts[1]));
        }
        observer.onNext(ReleaseReservationResponse.newBuilder().setReleased(reservation != null).build());
        observer.onCompleted();
    }
}
