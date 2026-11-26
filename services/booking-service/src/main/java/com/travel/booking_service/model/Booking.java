package com.travel.booking_service.model;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long travelId;

    @Column(length = 120)
    private String travelTitle;

    @Column(length = 20)
    private String paymentMethod;

    private LocalDate departureDate;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String country;

    private Integer duration;

    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(
            name = "booking_destinations",
            joinColumns = @jakarta.persistence.JoinColumn(name = "booking_id"))
    @jakarta.persistence.Column(name = "destination", length = 120)
    private java.util.List<String> destinations = new java.util.ArrayList<>();

    @Column(precision = 12, scale = 2)
    private BigDecimal priceMad;

    private Long managerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean feedbackSubmitted;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void setCreatedAt() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTravelId() {
        return travelId;
    }

    public void setTravelId(Long travelId) {
        this.travelId = travelId;
    }

    public String getTravelTitle() {
        return travelTitle;
    }

    public void setTravelTitle(String travelTitle) {
        this.travelTitle = travelTitle;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public java.util.List<String> getDestinations() {
        return destinations;
    }

    public void setDestinations(java.util.List<String> destinations) {
        this.destinations = destinations == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(destinations);
    }

    public BigDecimal getPriceMad() {
        return priceMad;
    }

    public void setPriceMad(BigDecimal priceMad) {
        this.priceMad = priceMad;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isFeedbackSubmitted() {
        return feedbackSubmitted;
    }

    public void setFeedbackSubmitted(boolean feedbackSubmitted) {
        this.feedbackSubmitted = feedbackSubmitted;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
