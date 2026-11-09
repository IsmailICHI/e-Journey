package com.travel.travel_service.services;

import org.neo4j.driver.Driver;
import org.springframework.stereotype.Service;

import com.travel.travel_service.events.BookingCreatedEvent;

@Service
public class BookingGraphService {

    private static final String UPSERT_BOOKING = """
            MERGE (traveler:Traveler {userId: $userId})
            MERGE (travel:Travel {travelId: $travelId})
            MERGE (booking:Booking {bookingId: $bookingId})
            ON CREATE SET booking.createdAt = datetime($createdAt)
            MERGE (traveler)-[:BOOKED]->(booking)
            MERGE (booking)-[:FOR_TRAVEL]->(travel)
            """;

    private final Driver driver;

    public BookingGraphService(Driver driver) {
        this.driver = driver;
    }

    public void recordBooking(BookingCreatedEvent event) {
        try (var session = driver.session()) {
            session.executeWrite(transaction -> {
                transaction.run(UPSERT_BOOKING, java.util.Map.of(
                        "userId", event.userId(),
                        "travelId", event.travelId(),
                        "bookingId", event.bookingId(),
                        "createdAt", event.createdAt().toString())).consume();
                return null;
            });
        }
    }
}
