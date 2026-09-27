package com.example.greatwalkalerts;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository
        extends JpaRepository<Facility, Integer> {

    List<Facility> findByWalk_IdOrderByIdAsc(int walkId);
}