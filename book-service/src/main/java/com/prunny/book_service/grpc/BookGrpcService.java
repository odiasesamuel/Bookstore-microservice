package com.prunny.book_service.grpc;

import book.BookServiceGrpc;
import book.ReduceAvailableCopiesRequest;
import book.ReduceAvailableCopiesResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class BookGrpcService extends BookServiceGrpc.BookServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BookGrpcService.class);

    @Override
    public void reduceAvailableCopies(book.ReduceAvailableCopiesRequest reduceAvailableCopiesRequest, StreamObserver<book.ReduceAvailableCopiesResponse> responseObserver) {
        log.info("ReduceAvailableCopies request received {}", reduceAvailableCopiesRequest.toString());

        // Business logic - e.g Save to db, perform calculations

        ReduceAvailableCopiesResponse response = ReduceAvailableCopiesResponse.newBuilder()
            .setSuccess(true)
            .setMessage("Successfully reduced available copies of book")
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
