package com.example.greatwalkalerts;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AlertRequest(
    @Positive int walkId,
    @NotNull @FutureOrPresent LocalDate startDate,
    @Positive int partySize
) {
}