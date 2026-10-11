package com.travel.travel_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long travelId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private Integer duration;

    @ElementCollection
    @CollectionTable(name = "travel_destinations", joinColumns = @JoinColumn(name = "travel_id"))
    @Column(name = "destination", nullable = false)
    @Builder.Default
    private List<String> destinations = new ArrayList<>();

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String accommodation;

    @Column(columnDefinition = "TEXT")
    private String transportation;

    @Column(length = 20)
    private String paymentMethod;

    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    @Builder.Default
    private List<ItineraryEntity> itinerary = new ArrayList<>();
}
