package com.example.greatwalkalerts;

import java.time.LocalDate;

public record AvailabilityResult(
    int facilityId,
    LocalDate date,
    int availableSpaces
) {
}