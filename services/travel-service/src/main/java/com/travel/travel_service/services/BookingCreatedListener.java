package com.travel.travel_service.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.travel.travel_service.events.BookingCreatedEvent;

@Component
public class BookingCreatedListener {

    private final BookingGraphService bookingGraphService;

    public BookingCreatedListener(BookingGraphService bookingGraphService) {
        this.bookingGraphService = bookingGraphService;
    }

    @KafkaListener(topics = "${app.booking.events-topic}")
    public void onBookingCreated(BookingCreatedEvent event) {
        bookingGraphService.recordBooking(event);
    }
}
