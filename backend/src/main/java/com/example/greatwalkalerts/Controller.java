package com.example.greatwalkalerts;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class Controller {

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
}