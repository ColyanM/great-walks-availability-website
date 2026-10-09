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

import org.springframework.web.bind.annotation.PatchMapping;

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
        private final AvailabilityService availabilityService;

        public Controller(AlertRepository alertRepository,
                        WalkRepository walkRepository,
                        FacilityRepository facilityRepository,
                        ItineraryRepository itineraryRepository,
                        ItineraryStopRepository itineraryStopRepository,
                        AvailabilityService availabilityService) {
                this.alertRepository = alertRepository;
                this.walkRepository = walkRepository;
                this.facilityRepository = facilityRepository;
                this.itineraryRepository = itineraryRepository;
                this.itineraryStopRepository = itineraryStopRepository;
                this.availabilityService = availabilityService;
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
                                .map(this::toAlertResponse)
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

                return toAlertResponse(savedAlert);
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

                return availabilityService.search(walkId, arrivalDate, nights);
        }

        @GetMapping("/api/itineraries/{id}/check")
        public TripMatchResponse checkItinerary(
                        @PathVariable int id,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam int partySize) {

                boolean matches = availabilityService.checkItinerary(
                                id,
                                startDate,
                                partySize);

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

                boolean matches = availabilityService.checkItinerary(
                                alert.getItineraryId(),
                                alert.getStartDate(),
                                alert.getPartySize());

                return new TripMatchResponse(
                                alert.getItineraryId(),
                                alert.getStartDate(),
                                alert.getPartySize(),
                                matches);
        }

        @PatchMapping("/api/alerts/{id}/status")
        public AlertResponse updateAlertStatus(
                        @PathVariable int id,
                        @Valid @RequestBody AlertStatusRequest request) {

                Alert alert = alertRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Alert not found."));

                if (request.active() && alert.getItineraryId() == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Choose an itinerary before activating this alert.");
                }

                alert.setActive(request.active());

                Alert savedAlert = alertRepository.save(alert);

                return toAlertResponse(savedAlert);
        }

        @PostMapping("/api/alerts/check-active")
        public List<AvailabilityService.AlertCheckResult> checkActiveAlerts() {
                return availabilityService.checkActiveAlerts();
        }

        private AlertResponse toAlertResponse(Alert alert) {
                return new AlertResponse(
                                alert.getId(),
                                alert.getWalkId(),
                                alert.getItineraryId(),
                                alert.getStartDate(),
                                alert.getPartySize(),
                                alert.isActive());
        }
}
