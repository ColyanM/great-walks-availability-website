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

@RestController
public class Controller {

    private final AlertRepository alertRepository;

public Controller(AlertRepository alertRepository) {
    this.alertRepository = alertRepository;
}

	@GetMapping("/api/health")
	public HealthResponse health() {
		return new HealthResponse("OK");
	}

	@GetMapping("/api/walks")
public List<WalksResponse> walks() {
    return List.of(
        new WalksResponse(1, "Kepler Track"),
        new WalksResponse(2, "Milford Track"),
        new WalksResponse(3, "Routeburn Track")
    );
}

@PostMapping("/api/alerts")
@ResponseStatus(HttpStatus.CREATED)
public AlertResponse receiveAlert(@Valid @RequestBody AlertRequest request) {
    boolean walkExists = walks().stream()
        .anyMatch(walk -> walk.id() == request.walkId());

    if (!walkExists) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Please choose a valid walk."
        );
    }

    Alert alert = new Alert(
        request.walkId(),
        request.startDate(),
        request.partySize()
    );

    Alert savedAlert = alertRepository.save(alert);

    return new AlertResponse(
        savedAlert.getId(),
        savedAlert.getWalkId(),
        savedAlert.getStartDate(),
        savedAlert.getPartySize()
    );
}
}