package com.travel.booking_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.travel.booking_service.dto.BookingResponse;
import com.travel.booking_service.dto.CreateBookingRequest;
import com.travel.booking_service.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBookingRequest request) {
        Object userIdClaim = jwt.getClaim("userId");
        if (!(userIdClaim instanceof Number userId) || userId.longValue() <= 0) {
            throw new IllegalStateException("Authenticated token does not contain a valid user ID");
        }
        return bookingService.createBooking(userId.longValue(), request);
    }
}
