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

@RestController
public class Controller {

    public record HealthResponse(String status) {
    }

    private final AlertRepository alertRepository;
    private final WalkRepository walkRepository;
    private final FacilityRepository facilityRepository;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryStopRepository itineraryStopRepository;
    private final DocAvailabilityProvider docAvailabilityProvider;

    public Controller(
            AlertRepository alertRepository,
            WalkRepository walkRepository,
            FacilityRepository facilityRepository,
            ItineraryRepository itineraryRepository,
            ItineraryStopRepository itineraryStopRepository,
            DocAvailabilityProvider docAvailabilityProvider) {
        this.alertRepository = alertRepository;
        this.walkRepository = walkRepository;
        this.facilityRepository = facilityRepository;
        this.itineraryRepository = itineraryRepository;
        this.itineraryStopRepository = itineraryStopRepository;
        this.docAvailabilityProvider = docAvailabilityProvider;
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

        Alert alert = new Alert(
                request.walkId(),
                request.startDate(),
                request.partySize());

        Alert savedAlert = alertRepository.save(alert);

        return new AlertResponse(
                savedAlert.getId(),
                savedAlert.getWalkId(),
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
    public DocAvailabilityResponse availability(
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

        try {
            return docAvailabilityProvider.search(
                    walk.getDocPlaceId(),
                    arrivalDate,
                    nights);
        } catch (RestClientException | IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not retrieve DOC availability. Try again later.",
                    exception);
        }
    }
}
