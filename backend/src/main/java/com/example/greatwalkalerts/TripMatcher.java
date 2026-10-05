package com.example.greatwalkalerts;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class TripMatcher {

    public boolean matches(
            List<ItineraryStop> stops,
            LocalDate startDate,
            int partySize,
            List<AvailabilityResult> availability) {

        Objects.requireNonNull(stops, "Itinerary stops are required");
        Objects.requireNonNull(startDate, "Start date is required");
        Objects.requireNonNull(availability, "Availability is required");

        if (partySize < 1) {
            throw new IllegalArgumentException(
                    "Party size must be positive");
        }

        if (stops.isEmpty()) {
            return false;
        }

        for (ItineraryStop stop : stops) {
            LocalDate requiredDate = startDate.plusDays(stop.getNightOffset());

            int requiredFacilityId = stop.getFacility().getId();

            boolean stopAvailable = false;

            for (AvailabilityResult result : availability) {
                if (result.facilityId() == requiredFacilityId
                        && result.date().equals(requiredDate)
                        && result.availableSpaces() >= partySize) {

                    stopAvailable = true;
                    break;
                }
            }

            if (!stopAvailable) {
                return false;
            }
        }

        return true;
    }
}