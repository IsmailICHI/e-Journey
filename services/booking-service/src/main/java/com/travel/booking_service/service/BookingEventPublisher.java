package com.travel.booking_service.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.booking_service.events.BookingCreatedEvent;
import com.travel.booking_service.model.BookingEventOutbox;
import com.travel.booking_service.repository.BookingEventOutboxRepository;

@Component
public class BookingEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BookingEventPublisher.class);
    private final BookingEventOutboxRepository outboxRepository;
    private final KafkaTemplate<String, BookingCreatedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public BookingEventPublisher(
            BookingEventOutboxRepository outboxRepository,
            KafkaTemplate<String, BookingCreatedEvent> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.booking.events-topic}") String topic) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
    public void publishPendingEvents() {
        List<BookingEventOutbox> pending = outboxRepository
                .findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 25));
        for (BookingEventOutbox outboxEvent : pending) {
            try {
                BookingCreatedEvent event = objectMapper.readValue(outboxEvent.getPayload(), BookingCreatedEvent.class);
                kafkaTemplate.send(topic, event.bookingId().toString(), event).get(10, TimeUnit.SECONDS);
                outboxEvent.setPublishedAt(Instant.now());
                outboxRepository.save(outboxEvent);
            } catch (Exception exception) {
                logger.error("Could not publish booking outbox event id={}; it will be retried",
                        outboxEvent.getId(), exception);
                break;
            }
        }
    }
}
