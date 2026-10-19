package com.portfolio.orders.infrastructure.grpc;

import com.portfolio.contracts.inventory.v1.InventoryServiceGrpc;
import com.portfolio.contracts.payment.v1.PaymentServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfiguration {
    @Bean
    public InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub(GrpcChannelFactory channels) {
        return InventoryServiceGrpc.newBlockingStub(channels.createChannel("static://localhost:9091"));
    }

    @Bean
    public PaymentServiceGrpc.PaymentServiceBlockingStub paymentStub(GrpcChannelFactory channels) {
        return PaymentServiceGrpc.newBlockingStub(channels.createChannel("static://localhost:9092"));
    }
}
