package com.prunny.book_service.grpc.server;

import book.BookServiceGrpc;
import book.ReduceAvailableCopiesRequest;
import book.ReduceAvailableCopiesResponse;
import com.prunny.book_service.domain.Book;
import com.prunny.book_service.repository.BookRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@GrpcService
public class BookGrpcService extends BookServiceGrpc.BookServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BookGrpcService.class);

    private final BookRepository bookRepository;

    public BookGrpcService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void reduceAvailableCopies(book.ReduceAvailableCopiesRequest request, StreamObserver<book.ReduceAvailableCopiesResponse> responseObserver) {
        log.info("ReduceAvailableCopies request received {}", request.toString());

        try {
            Optional<Book> bookOptional = bookRepository.findByIsbn(request.getBookIsbn());
            if (bookOptional.isEmpty()) {
                ReduceAvailableCopiesResponse response = ReduceAvailableCopiesResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Book not found")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Book book = bookOptional.get();
            if (book.getAvailableCopies() < request.getQuantity()) {
                ReduceAvailableCopiesResponse response = ReduceAvailableCopiesResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Not enough stock")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            book.setAvailableCopies(book.getAvailableCopies() - request.getQuantity());
            bookRepository.save(book);

            ReduceAvailableCopiesResponse response = ReduceAvailableCopiesResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Successfully reduced available copies of book")
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error while reducing available copies", e);
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Unexpected error occurred")
                    .withCause(e)
                    .asRuntimeException()
            );
        }
    }

}
