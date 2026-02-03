package com.prunny.order_service.event.publisher;

import com.bookstore.events.BookOrderedEvent;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class BookEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(BookEventPublisher.class);

    private final KafkaTemplate<String, BookOrderedEvent> kafkaTemplate;
    private final String topicName;

    public BookEventPublisher(
        KafkaTemplate<String, BookOrderedEvent> kafkaTemplate,
        @Value("${bookstore.kafka.topics.book-ordered}") String topicName)
    {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    // Asynchronous - fire and forget with callback
    public void publishBookOrderedEvent(String orderId, String bookIsbn, Integer quantity) {

        // Build the Protobuf message
        Instant now = Instant.now();
        BookOrderedEvent event = BookOrderedEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())  // Unique ID for this event
            .setOrderId(orderId)
            .setBookIsbn(bookIsbn)
            .setQuantity(quantity)
            .setOrderedAt(Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build())
            .build();

        LOG.info("Publishing BookOrderedEvent: orderId={}, bookIsbn={}, quantity={}",
            orderId, bookIsbn, quantity);

        // Send to Kafka
        // Key = bookIsbn (messages with same ISBN go to same partition)
        // Value = the event
        CompletableFuture<SendResult<String, BookOrderedEvent>> future =
            kafkaTemplate.send(topicName, bookIsbn, event);

        // Handle success/failure asynchronously
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                LOG.info("Successfully published event: eventId={}, partition={}, offset={}",
                    event.getEventId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                LOG.error("Failed to publish event: eventId={}, error={}",
                    event.getEventId(), ex.getMessage(), ex);
            }
        });
    }


    // Synchronous - blocks until Kafka acknowledges receipt
//    public void publishBookOrderedEventSync(String orderId, String bookIsbn, Integer quantity) {
//        Instant now = Instant.now();
//        BookOrderedEvent event = BookOrderedEvent.newBuilder()
//            .setEventId(UUID.randomUUID().toString())
//            .setOrderId(orderId)
//            .setBookIsbn(bookIsbn)
//            .setQuantity(quantity)
//            .setOrderedAt(Timestamp.newBuilder()
//                .setSeconds(now.getEpochSecond())
//                .setNanos(now.getNano())
//                .build())
//            .build();
//
//        LOG.info("Publishing BookOrderedEvent: orderId={}, bookIsbn={}, quantity={}",
//            orderId, bookIsbn, quantity);
//
//        try {
//            // Block and wait for result
//            SendResult<String, BookOrderedEvent> result =
//                kafkaTemplate.send(topicName, bookIsbn, event).get();
//
//            LOG.info("Successfully published event: eventId={}, partition={}, offset={}",
//                event.getEventId(),
//                result.getRecordMetadata().partition(),
//                result.getRecordMetadata().offset());
//
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            LOG.error("Thread interrupted while publishing event: eventId={}",
//                event.getEventId(), e);
//            throw new RuntimeException("Event publishing interrupted", e);
//        } catch (ExecutionException e) {
//            LOG.error("Failed to publish event: eventId={}, error={}",
//                event.getEventId(), e.getCause().getMessage(), e.getCause());
//            throw new RuntimeException("Event publishing failed", e.getCause());
//        }
//    }
}
