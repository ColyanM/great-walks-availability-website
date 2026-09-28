package com.example.greatwalkalerts;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryStopRepository
        extends JpaRepository<ItineraryStop, Integer> {

    @EntityGraph(attributePaths = "facility")
    List<ItineraryStop> findByItinerary_IdOrderByNightOffsetAsc(
            int itineraryId);
}