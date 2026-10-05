package com.example.greatwalkalerts;

import java.time.LocalDate;

public record AlertResponse(
        int id,
        int walkId,
        Integer itineraryId,
        LocalDate startDate,
        int partySize) {
}