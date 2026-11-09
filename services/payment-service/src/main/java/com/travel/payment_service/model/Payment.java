package com.travel.payment_service.model;

import java.math.BigDecimal;
import java.time.Instant;

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
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long travelId;

    private Long managerId;

    @Column(nullable = false, length = 120)
    private String travelTitle;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String provider = "PAYPAL";

    @Column(unique = true)
    private String providerOrderId;

    @Column(length = 1000)
    private String approvalUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    protected Payment() {
    }

    public Payment(Long bookingId, Long userId, Long travelId, Long managerId,
            String travelTitle, BigDecimal amount, String currency, String providerOrderId, String approvalUrl) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.travelId = travelId;
        this.managerId = managerId;
        this.travelTitle = travelTitle;
        this.amount = amount;
        this.currency = currency;
        this.providerOrderId = providerOrderId;
        this.approvalUrl = approvalUrl;
    }

    @PrePersist
    void setCreatedAt() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public String getApprovalUrl() { return approvalUrl; }
    public Long getTravelId() { return travelId; }
    public Long getManagerId() { return managerId; }
    public String getTravelTitle() { return travelTitle; }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() { return completedAt; }

    public void complete() {
        status = PaymentStatus.COMPLETED;
        completedAt = Instant.now();
    }
}
