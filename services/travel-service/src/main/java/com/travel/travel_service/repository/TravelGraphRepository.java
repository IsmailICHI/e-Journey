package com.travel.travel_service.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.travel.travel_service.models.TravelGraphNode;

public interface TravelGraphRepository extends Neo4jRepository<TravelGraphNode, Long> {

    @Query("""
            MERGE (travel:Travel {travelId: $travelId})
            SET travel.city = $city,
                travel.country = $country,
                travel.duration = $duration,
                travel.paymentMethod = $paymentMethod,
                travel.departureDate = $departureDate,
                travel.available = true
            """)
    void upsertTravel(
            @Param("travelId") Long travelId,
            @Param("city") String city,
            @Param("country") String country,
            @Param("duration") Integer duration,
            @Param("paymentMethod") String paymentMethod,
            @Param("departureDate") LocalDate departureDate);

    @Query("""
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
            """)
    void upsertBooking(
            @Param("userId") Long userId,
            @Param("travelId") Long travelId,
            @Param("bookingId") Long bookingId,
            @Param("createdAt") String createdAt,
            @Param("bookingStatus") String bookingStatus,
            @Param("city") String city,
            @Param("country") String country,
            @Param("duration") Integer duration,
            @Param("paymentMethod") String paymentMethod);

    @Query("""
            MERGE (booking:Booking {bookingId: $bookingId})
            SET booking.status = $bookingStatus
            """)
    void updateBookingStatus(
            @Param("bookingId") Long bookingId,
            @Param("bookingStatus") String bookingStatus);

    @Query("""
            MATCH (booking:Booking {bookingId: $bookingId})
            SET booking.rating = $rating
            """)
    void updateFeedback(@Param("bookingId") Long bookingId, @Param("rating") Integer rating);

    @Query("""
            MATCH (traveler:Traveler {userId: $userId})-[:BOOKED]->(pastBooking:Booking {status: 'CONFIRMED'})
                  -[:FOR_TRAVEL]->(past:Travel)
            MATCH (candidate:Travel {available: true})
            WHERE candidate.travelId <> past.travelId
              AND (candidate.departureDate IS NULL OR date(candidate.departureDate) >= date())
              AND NOT EXISTS {
                  MATCH (traveler)-[:BOOKED]->(:Booking)-[:FOR_TRAVEL]->(candidate)
              }
            WITH candidate, past,
                 (CASE WHEN candidate.city = past.city THEN 2 ELSE 0 END
                  + CASE WHEN candidate.country = past.country THEN 1 ELSE 0 END
                  + CASE WHEN candidate.duration = past.duration THEN 1 ELSE 0 END
                  + CASE WHEN candidate.paymentMethod = past.paymentMethod THEN 1 ELSE 0 END)
                  AS score
            WITH candidate, sum(score) AS score
            WHERE score >= 2
            RETURN candidate.travelId AS travelId
            ORDER BY score DESC, candidate.travelId ASC
            LIMIT $limit
            """)
    List<Long> recommendTravels(@Param("userId") Long userId, @Param("limit") int limit);

    @Query("""
            MATCH (travel:Travel {travelId: $travelId})
            SET travel.available = false
            """)
    void markTravelUnavailable(@Param("travelId") Long travelId);
}
