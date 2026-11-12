package com.travel.travel_service.services;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import com.travel.travel_service.dto.TravelResponse;
import com.travel.travel_service.events.BookingCreatedEvent;

@Service
public class BookingGraphService {

    private static final String UPSERT_TRAVEL = """
            MERGE (travel:Travel {travelId: $travelId})
            SET travel.city = $city,
                travel.country = $country,
                travel.duration = $duration,
                travel.paymentMethod = $paymentMethod,
                travel.departureDate = $departureDate,
                travel.available = $available
            """;

    private static final String UPSERT_BOOKING = """
            MERGE (traveler:Traveler {userId: $userId})
            MERGE (travel:Travel {travelId: $travelId})
            MERGE (booking:Booking {bookingId: $bookingId})
            SET booking.createdAt = coalesce(booking.createdAt, datetime($createdAt)),
                booking.status = coalesce($bookingStatus, booking.status, 'PENDING_PAYMENT'),
                booking.city = coalesce($city, booking.city),
                booking.country = coalesce($country, booking.country),
                booking.duration = coalesce($duration, booking.duration),
                booking.paymentMethod = coalesce($paymentMethod, booking.paymentMethod)
            MERGE (traveler)-[:BOOKED]->(booking)
            MERGE (booking)-[:FOR_TRAVEL]->(travel)
            """;

    private static final String UPDATE_BOOKING_STATUS = """
            MERGE (booking:Booking {bookingId: $bookingId})
            SET booking.status = $bookingStatus
            """;

    private static final String UPDATE_FEEDBACK = """
            MATCH (booking:Booking {bookingId: $bookingId})
            SET booking.rating = $rating
            """;

    private static final String RECOMMEND_TRAVELS = """
            MATCH (traveler:Traveler {userId: $userId})-[:BOOKED]->(pastBooking:Booking {status: 'CONFIRMED'})
                  -[:FOR_TRAVEL]->(past:Travel)
            MATCH (candidate:Travel {available: true})
            WHERE candidate.travelId <> past.travelId
              AND (candidate.departureDate IS NULL OR date(candidate.departureDate) >= date())
              AND NOT EXISTS {
                  MATCH (traveler)-[:BOOKED]->(:Booking)-[:FOR_TRAVEL]->(candidate)
              }
            WITH candidate, past, pastBooking,
                 (CASE WHEN candidate.city = past.city THEN 2 ELSE 0 END
                  + CASE WHEN candidate.country = past.country THEN 1 ELSE 0 END
                  + CASE WHEN candidate.duration = past.duration THEN 1 ELSE 0 END
                  + CASE WHEN candidate.paymentMethod = past.paymentMethod THEN 1 ELSE 0 END)
                  * coalesce(pastBooking.rating, 3) / 5.0 AS score
            WITH candidate, sum(score) AS score
            WHERE score > 0
            RETURN candidate.travelId AS travelId
            ORDER BY score DESC, candidate.travelId ASC
            LIMIT $limit
            """;

    private static final String DELETE_TRAVEL = """
            MATCH (travel:Travel {travelId: $travelId})
            SET travel.available = false
            """;

    private final Driver driver;

    public BookingGraphService(Driver driver) {
        this.driver = driver;
    }

    public void recordBooking(BookingCreatedEvent event) {
        try (var session = driver.session()) {
            session.executeWrite(transaction -> {
                if ("FEEDBACK_SUBMITTED".equals(event.eventType())) {
                    transaction.run(UPDATE_FEEDBACK, java.util.Map.of(
                            "bookingId", event.bookingId(),
                            "rating", event.rating())).consume();
                } else if ("BOOKING_STATUS_UPDATED".equals(event.eventType())) {
                    transaction.run(UPDATE_BOOKING_STATUS, java.util.Map.of(
                            "bookingId", event.bookingId(),
                            "bookingStatus", event.bookingStatus())).consume();
                } else {
                    java.util.Map<String, Object> parameters = new java.util.HashMap<>();
                    parameters.put("userId", event.userId());
                    parameters.put("travelId", event.travelId());
                    parameters.put("bookingId", event.bookingId());
                    parameters.put("createdAt", event.createdAt().toString());
                    parameters.put("bookingStatus", event.bookingStatus());
                    parameters.put("city", event.city());
                    parameters.put("country", event.country());
                    parameters.put("duration", event.duration());
                    parameters.put("paymentMethod", event.paymentMethod());
                    transaction.run(UPSERT_BOOKING, parameters).consume();
                }
                return null;
            });
        }
    }

    public void indexTravel(TravelResponse travel) {
        java.util.Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("travelId", travel.travelId());
        parameters.put("city", travel.city());
        parameters.put("country", travel.country());
        parameters.put("duration", travel.duration());
        parameters.put("paymentMethod", travel.paymentMethod());
        parameters.put("departureDate", travel.startDate() == null ? null : travel.startDate().toString());
        parameters.put("available", true);
        try (var session = driver.session()) {
            session.executeWrite(transaction -> {
                transaction.run(UPSERT_TRAVEL, parameters).consume();
                return null;
            });
        }
    }

    public void deleteTravel(Long travelId) {
        try (var session = driver.session()) {
            session.executeWrite(transaction -> {
                transaction.run(DELETE_TRAVEL, java.util.Map.of("travelId", travelId)).consume();
                return null;
            });
        }
    }

    public java.util.List<Long> recommendTravels(Long userId, int limit) {
        try (var session = driver.session()) {
            return session.executeRead(transaction -> transaction.run(
                            RECOMMEND_TRAVELS,
                            java.util.Map.of("userId", userId, "limit", Math.max(1, Math.min(limit, 20))))
                    .list(record -> record.get("travelId").asLong()));
        }
    }
}
