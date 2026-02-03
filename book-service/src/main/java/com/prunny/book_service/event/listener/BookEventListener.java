package com.prunny.book_service.event.listener;

import com.bookstore.events.BookOrderedEvent;
import com.prunny.book_service.service.BookService;
import com.prunny.book_service.service.EventIdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class BookEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(BookEventListener.class);

    private final BookService bookService;

    private final EventIdempotencyService eventIdempotencyService;

    public BookEventListener(BookService bookService, EventIdempotencyService eventIdempotencyService) {
        this.bookService = bookService;
        this.eventIdempotencyService = eventIdempotencyService;
    }

    // This method is called whenever a message arrives on the topic
    @KafkaListener(
        topics = "${bookstore.kafka.topics.book-ordered}",  // Which topic to listen to
        groupId = "${spring.kafka.consumer.group-id}"       // Which consumer group
    )
    public void handleBookOrderedEvent(
        @Payload BookOrderedEvent event,                    // The message content
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,  // Which partition
        @Header(KafkaHeaders.OFFSET) long offset,           // Position in partition
        Acknowledgment acknowledgment) {                    // For manual commit

        try {
            LOG.info("Received BookOrderedEvent: eventId={}, orderId={}, bookIsbn={}, quantity={}, partition={}, offset={}", event.getEventId(), event.getOrderId(), event.getBookIsbn(), event.getQuantity(), partition, offset);

            // CHECK: Have we seen this event before?
            if (eventIdempotencyService.isEventProcessed(event.getEventId())) {
                LOG.warn("Event already processed, skipping: eventId={}", event.getEventId());
                acknowledgment.acknowledge();  // Acknowledge but skip processing
                return;
            }

            // Do the actual work - update sales count
            bookService.incrementSalesCount(event.getBookIsbn(), event.getQuantity());

            // Mark as processed
            eventIdempotencyService.markEventAsProcessed(event.getEventId());

            // Tell Kafka we successfully processed this message
            acknowledgment.acknowledge();

            LOG.info("Successfully processed BookOrderedEvent: eventId={}", event.getEventId());

        } catch (Exception ex) {
            LOG.error("Error processing BookOrderedEvent: eventId={}, error={}",
                event.getEventId(), ex.getMessage(), ex);

            // DON'T acknowledge - Kafka will redeliver this message
            // This ensures at-least-once delivery
            throw ex;
        }
    }
}
