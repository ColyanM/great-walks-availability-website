package com.example.greatwalkalerts;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DocAvailabilityProvider {

    private final RestClient client;

    public DocAvailabilityProvider() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(20));

        this.client = RestClient.builder()
                .baseUrl(
                        "https://prod-nz-rdr.recreation-management.tylerapp.com")
                // DOC challenges the default Java client identifier.
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (compatible; GreatWalkAlerts/0.1)")
                .requestFactory(requestFactory)
                .build();
    }

    public DocAvailabilityResponse search(
            int docPlaceId, LocalDate arrivalDate, int nights) {

        Objects.requireNonNull(arrivalDate, "Arrival date is required");

        if (docPlaceId <= 0 || nights < 1) {
            throw new IllegalArgumentException(
                    "Place ID and nights must be positive");
        }

        SearchRequest request = new SearchRequest(
                "",
                docPlaceId,
                0,
                arrivalDate,
                nights);

        DocAvailabilityResponse response = client.post()
                .uri("/nzrdr/rdr/search/greatwalkplacefacility")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(
                        status -> status.value() != 200,
                        (requestDetails, responseDetails) -> {
                            String wafAction = responseDetails.getHeaders()
                                    .getFirst("x-amzn-waf-action");

                            if ("challenge".equals(wafAction)) {
                                throw new IllegalStateException(
                                        "DOC requested browser verification; "
                                                + "availability could not be retrieved");
                            }

                            throw new IllegalStateException(
                                    "Unexpected DOC response: "
                                            + responseDetails.getStatusCode());
                        })
                .body(DocAvailabilityResponse.class);

        if (response == null || response.facilities() == null) {
            throw new IllegalStateException(
                    "DOC returned a response without facility data");
        }

        return response;
    }

    private record SearchRequest(
            @JsonProperty("accomodation") String accommodation,
            int placeId,
            int customerClassificationId,
            LocalDate arrivalDate,
            int nights) {
    }
}
