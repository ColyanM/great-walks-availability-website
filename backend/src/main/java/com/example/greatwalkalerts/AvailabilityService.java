package com.example.greatwalkalerts;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.ArrayList;

@Service
public class AvailabilityService {

    private final WalkRepository walkRepository;
    private final FacilityRepository facilityRepository;
    private final DocAvailabilityProvider docAvailabilityProvider;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryStopRepository itineraryStopRepository;
    private final AlertRepository alertRepository;

    private record AvailabilityWindow(
            int walkId,
            LocalDate startDate,
            int nights) {
    }

    public record AlertCheckResult(
            int alertId,
            Boolean matches,
            String error) {
    }

    public AvailabilityService(
            WalkRepository walkRepository,
            FacilityRepository facilityRepository,
            DocAvailabilityProvider docAvailabilityProvider,
            ItineraryRepository itineraryRepository,
            ItineraryStopRepository itineraryStopRepository,
            AlertRepository alertRepository) {

        this.walkRepository = walkRepository;
        this.facilityRepository = facilityRepository;
        this.docAvailabilityProvider = docAvailabilityProvider;
        this.itineraryRepository = itineraryRepository;
        this.itineraryStopRepository = itineraryStopRepository;
        this.alertRepository = alertRepository;
    }

    public List<AlertCheckResult> checkActiveAlerts() {
        LocalDate today = LocalDate.now(ZoneId.of("Pacific/Auckland"));

        List<Alert> alerts = alertRepository
                .findByActiveTrueAndItineraryIdIsNotNullAndStartDateGreaterThanEqualOrderByIdAsc(
                        today);

        Map<AvailabilityWindow, List<AvailabilityResult>> fetchedAvailability = new HashMap<>();

        List<AlertCheckResult> results = new ArrayList<>();

        for (Alert alert : alerts) {
            try {
                boolean matches = checkItinerary(
                        alert.getItineraryId(),
                        alert.getStartDate(),
                        alert.getPartySize(),
                        fetchedAvailability);

                results.add(new AlertCheckResult(
                        alert.getId(),
                        matches,
                        null));

            } catch (ResponseStatusException exception) {
                results.add(new AlertCheckResult(
                        alert.getId(),
                        null,
                        exception.getReason()));
            }
        }

        return results;
    }

    public boolean checkItinerary(
            int itineraryId,
            LocalDate startDate,
            int partySize) {

        return checkItinerary(
                itineraryId,
                startDate,
                partySize,
                new HashMap<>());
    }

    private boolean checkItinerary(
            int itineraryId,
            LocalDate startDate,
            int partySize,
            Map<AvailabilityWindow, List<AvailabilityResult>> fetchedAvailability) {

        Objects.requireNonNull(startDate, "Start date is required");

        if (partySize < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Party size must be positive.");
        }

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Itinerary not found."));

        List<ItineraryStop> stops = itineraryStopRepository
                .findByItinerary_IdOrderByNightOffsetAsc(itineraryId);

        if (stops.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This itinerary has no overnight stops.");
        }

        for (ItineraryStop stop : stops) {
            if (stop.getFacility().getDocFacilityId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Availability is not configured for every stop.");
            }
        }

        int nights = stops.getLast().getNightOffset() + 1;

        AvailabilityWindow window = new AvailabilityWindow(
                itinerary.getWalk().getId(),
                startDate,
                nights);

        List<AvailabilityResult> results = fetchedAvailability.get(window);

        if (results == null) {
            results = search(
                    window.walkId(),
                    window.startDate(),
                    window.nights());

            fetchedAvailability.put(window, results);
        }

        return matchesTrip(stops, startDate, partySize, results);
    }

    private static boolean matchesTrip(
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

    public List<AvailabilityResult> search(
            int walkId,
            LocalDate arrivalDate,
            int nights) {

        Objects.requireNonNull(arrivalDate, "Arrival date is required");

        if (nights < 1 || nights > 11) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nights must be between 1 and 11.");
        }

        Walk walk = walkRepository.findById(walkId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Walk not found."));

        if (walk.getDocPlaceId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Availability is not configured for this walk yet.");
        }

        List<Facility> facilities = facilityRepository.findByWalk_IdOrderByIdAsc(walkId);

        Map<Integer, Integer> localIdsByDocId = new HashMap<>();

        for (Facility facility : facilities) {
            Integer docFacilityId = facility.getDocFacilityId();

            if (docFacilityId != null) {
                localIdsByDocId.put(docFacilityId, facility.getId());
            }
        }

        try {
            DocAvailabilityResponse response = docAvailabilityProvider.search(
                    walk.getDocPlaceId(),
                    arrivalDate,
                    nights);

            return DocAvailabilityProvider.mapAvailability(
                    response,
                    localIdsByDocId);

        } catch (RestClientException | IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not retrieve DOC availability. Try again later.",
                    exception);
        }
    }
}
