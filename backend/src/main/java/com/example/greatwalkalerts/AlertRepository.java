package com.example.greatwalkalerts;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Integer> {

    List<Alert> findByActiveTrueAndItineraryIdIsNotNullAndStartDateGreaterThanEqualOrderByIdAsc(
            LocalDate earliestDate);
}