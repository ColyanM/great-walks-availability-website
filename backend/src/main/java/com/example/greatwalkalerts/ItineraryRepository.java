package com.example.greatwalkalerts;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryRepository
        extends JpaRepository<Itinerary, Integer> {

    List<Itinerary> findByWalk_IdOrderByIdAsc(int walkId);
}