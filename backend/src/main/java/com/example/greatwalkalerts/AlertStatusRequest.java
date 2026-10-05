package com.example.greatwalkalerts;

import jakarta.validation.constraints.NotNull;

public record AlertStatusRequest(
        @NotNull Boolean active) {
}