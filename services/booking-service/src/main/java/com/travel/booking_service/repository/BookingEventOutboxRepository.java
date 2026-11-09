package com.travel.booking_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.travel.booking_service.model.BookingEventOutbox;

public interface BookingEventOutboxRepository extends JpaRepository<BookingEventOutbox, Long> {

    List<BookingEventOutbox> findByPublishedAtIsNullOrderByCreatedAtAsc(Pageable pageable);
}
