package com.prunny.order_service.grpc.client;


import book.BookServiceGrpc;
import book.ReduceAvailableCopiesRequest;
import book.ReduceAvailableCopiesResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BookServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(BookServiceGrpcClient.class);
    private final BookServiceGrpc.BookServiceBlockingStub blockingStub;

    public BookServiceGrpcClient(
        @Value("${book.service.address:localhost}") String serverAddress,
        @Value("${book.service.grpc.port:9000}") int serverPort,
        ObservationRegistry observationRegistry
    ) {
        log.info("Connecting to Billing Service GRPC service at {}:{}", serverAddress, serverPort);

//        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort).usePlaintext().build();

        /**
         * Problem: Version Mismatch
         * grpc-spring-boot-starter:3.1.0.RELEASE is built for older gRPC versions (e.g., ~1.47.0).
         * I am using grpc-java:1.69.0, which is too new for the starter to detect the transport classes correctly.
         * This mismatch causes the starter or ManagedChannelBuilder to fail in locating the transport implementations (NettyChannelProvider, etc.) — even though you included grpc-netty-shaded
         *
         * Solution Options
         * Option 1: Downgrade gRPC to match the starter i.e use version 1.47.0 - 1.58.0
         * Option 2: Remove the starter and configure gRPC manually: stay on 1.69.0 (latest), remove grpc-spring-boot-starter and configure the gRPC client manually
         *
         *  I Implemented option 2
         * */

        ManagedChannel channel = NettyChannelBuilder
            .forAddress(serverAddress, serverPort)
            .usePlaintext()
            .intercept(new ObservationGrpcClientInterceptor(observationRegistry))
            .build();

        blockingStub = BookServiceGrpc.newBlockingStub(channel);
    }

    public ReduceAvailableCopiesResponse reduceAvailableCopies(String bookIsbn, int quantity) {
        ReduceAvailableCopiesRequest request = ReduceAvailableCopiesRequest.newBuilder().setBookIsbn(bookIsbn).setQuantity(quantity).build();

        ReduceAvailableCopiesResponse response = blockingStub.reduceAvailableCopies(request);

        return response;
    }
}
