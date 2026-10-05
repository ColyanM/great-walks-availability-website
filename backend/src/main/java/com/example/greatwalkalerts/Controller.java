package com.example.greatwalkalerts;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@RestController
public class Controller {

    public record HealthResponse(String status) {
    }

    public record TripMatchResponse(
            int itineraryId,
            LocalDate startDate,
            int partySize,
            boolean matches) {
    }

    private final AlertRepository alertRepository;
    private final WalkRepository walkRepository;
    private final FacilityRepository facilityRepository;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryStopRepository itineraryStopRepository;
    private final DocAvailabilityProvider docAvailabilityProvider;
    private final TripMatcher tripMatcher;

    public Controller(
            AlertRepository alertRepository,
            WalkRepository walkRepository,
            FacilityRepository facilityRepository,
            ItineraryRepository itineraryRepository,
            ItineraryStopRepository itineraryStopRepository,
            DocAvailabilityProvider docAvailabilityProvider, TripMatcher tripMatcher) {
        this.alertRepository = alertRepository;
        this.walkRepository = walkRepository;
        this.facilityRepository = facilityRepository;
        this.itineraryRepository = itineraryRepository;
        this.itineraryStopRepository = itineraryStopRepository;
        this.docAvailabilityProvider = docAvailabilityProvider;
        this.tripMatcher = tripMatcher;
    }

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("OK");
    }

    @GetMapping("/api/walks")
    public List<WalksResponse> walks() {
        return walkRepository.findAll(Sort.by("id"))
                .stream()
                .map(walk -> new WalksResponse(
                        walk.getId(),
                        walk.getName()))
                .toList();
    }

    @GetMapping("/api/alerts")
    public List<AlertResponse> alerts() {
        return alertRepository.findAll(Sort.by("id").descending())
                .stream()
                .map(alert -> new AlertResponse(
                        alert.getId(),
                        alert.getWalkId(),
                        alert.getItineraryId(),
                        alert.getStartDate(),
                        alert.getPartySize()))
                .toList();
    }

    @PostMapping("/api/alerts")
    @ResponseStatus(HttpStatus.CREATED)
    public AlertResponse receiveAlert(@Valid @RequestBody AlertRequest request) {
        boolean walkExists = walkRepository.existsById(request.walkId());

        if (!walkExists) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please choose a valid walk.");
        }

        Itinerary itinerary = itineraryRepository
                .findById(request.itineraryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Please choose a valid itinerary."));

        if (itinerary.getWalk().getId() != request.walkId()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The itinerary must belong to the selected walk.");
        }

        Alert alert = new Alert(
                request.walkId(),
                request.startDate(),
                request.partySize());

        alert.setItineraryId(itinerary.getId());

        Alert savedAlert = alertRepository.save(alert);

        return new AlertResponse(
                savedAlert.getId(),
                savedAlert.getWalkId(),
                savedAlert.getItineraryId(),
                savedAlert.getStartDate(),
                savedAlert.getPartySize());
    }

    @DeleteMapping("/api/alerts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlert(@PathVariable int id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Alert not found."));

        alertRepository.delete(alert);
    }

    @GetMapping("/api/walks/{walkId}/facilities")
    public List<FacilityResponse> facilities(@PathVariable int walkId) {
        if (!walkRepository.existsById(walkId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Walk not found.");
        }

        return facilityRepository.findByWalk_IdOrderByIdAsc(walkId)
                .stream()
                .map(facility -> new FacilityResponse(
                        facility.getId(),
                        facility.getName(),
                        facility.getFacilityType()))
                .toList();
    }

    @GetMapping("/api/itineraries/{id}")
    public ItineraryResponse itinerary(@PathVariable int id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Itinerary not found."));

        List<ItineraryResponse.Stop> stops = itineraryStopRepository
                .findByItinerary_IdOrderByNightOffsetAsc(id)
                .stream()
                .map(stop -> {
                    Facility facility = stop.getFacility();

                    return new ItineraryResponse.Stop(
                            stop.getNightOffset(),
                            new FacilityResponse(
                                    facility.getId(),
                                    facility.getName(),
                                    facility.getFacilityType()));
                })
                .toList();

        return new ItineraryResponse(
                itinerary.getId(),
                itinerary.getName(),
                stops);
    }

    @GetMapping("/api/walks/{walkId}/availability")
    public List<AvailabilityResult> availability(
            @PathVariable int walkId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalDate,
            @RequestParam(defaultValue = "11") int nights) {

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

    @GetMapping("/api/itineraries/{id}/check")
    public TripMatchResponse checkItinerary(
            @PathVariable int id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam int partySize) {

        if (partySize < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Party size must be positive.");
        }

        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Itinerary not found."));

        List<ItineraryStop> stops = itineraryStopRepository
                .findByItinerary_IdOrderByNightOffsetAsc(id);

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

        List<AvailabilityResult> results = availability(
                itinerary.getWalk().getId(),
                startDate,
                nights);

        boolean matches = tripMatcher.matches(
                stops,
                startDate,
                partySize,
                results);

        return new TripMatchResponse(
                id,
                startDate,
                partySize,
                matches);
    }

    @GetMapping("/api/alerts/{id}/check")
    public TripMatchResponse checkAlert(@PathVariable int id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Alert not found."));

        if (alert.getItineraryId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This alert needs an itinerary before it can be checked.");
        }

        return checkItinerary(
                alert.getItineraryId(),
                alert.getStartDate(),
                alert.getPartySize());
    }
}
