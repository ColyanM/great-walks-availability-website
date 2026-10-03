package com.example.greatwalkalerts;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocAvailabilityResponse(
        @JsonProperty("GreatWalkFacilityData") List<FacilityData> facilities) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FacilityData(
            @JsonProperty("FacilityId") Integer facilityId,

            @JsonProperty("FacilityName") String facilityName,

            @JsonProperty("IsAvailableForPatron") Boolean availableForPatron,

            @JsonProperty("GreatWalkFacilityDateData") List<DateData> dates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DateData(
            @JsonProperty("ArrivalDate") LocalDateTime arrivalDate,

            @JsonProperty("TotalAvailable") Integer totalAvailable,

            @JsonProperty("IsSeasonAvailable") Boolean seasonAvailable,

            @JsonProperty("IsAvailable") Boolean available) {
    }
}