package com.travel.travel_service.models;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "itineraries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "itinerary_activities", joinColumns = @JoinColumn(name = "itinerary_id"))
    @Column(name = "activity", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> activities = new ArrayList<>();

    @ManyToOne()
    @JoinColumn(name = "travel_id", nullable = false)
    private TravelEntity travel;
}
