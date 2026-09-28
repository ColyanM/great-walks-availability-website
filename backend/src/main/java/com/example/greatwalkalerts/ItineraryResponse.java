package com.example.greatwalkalerts;

import java.util.List;

public record ItineraryResponse(
    int id,
    String name,
    List<ItineraryStopResponse> stops
) {
}