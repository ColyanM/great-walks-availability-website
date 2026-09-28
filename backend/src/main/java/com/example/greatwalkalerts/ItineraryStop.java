package com.example.greatwalkalerts;

import jakarta.persistence.*;

@Entity
@Table(name = "itinerary_stops")
public class ItineraryStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "night_offset", nullable = false)
    private int nightOffset;

    protected ItineraryStop() {
}

public Integer getId() {
    return id;
}

public Itinerary getItinerary() {
    return itinerary;
}

public Facility getFacility() {
    return facility;
}

public int getNightOffset() {
    return nightOffset;
}
}