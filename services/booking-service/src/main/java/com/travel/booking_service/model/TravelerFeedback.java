package com.travel.booking_service.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "traveler_feedback")
public class TravelerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    @Column(nullable = false)
    private Long travelerId;

    @Column(nullable = false)
    private Long travelId;

    private Long managerId;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 2000)
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TravelerFeedback() {
    }

    public TravelerFeedback(Booking booking, Integer rating, String comment) {
        this.bookingId = booking.getId();
        this.travelerId = booking.getUserId();
        this.travelId = booking.getTravelId();
        this.managerId = booking.getManagerId();
        this.rating = rating;
        this.comment = comment;
    }

    @PrePersist
    void setCreatedAt() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public Long getTravelerId() { return travelerId; }
    public Long getTravelId() { return travelId; }
    public Long getManagerId() { return managerId; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
