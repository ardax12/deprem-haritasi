package com.ag.deprem.service;

import com.ag.deprem.model.dto.AfadRawEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
public class AfadApiClient {

    private static final Logger log = LoggerFactory.getLogger(AfadApiClient.class);
    private static final DateTimeFormatter AFAD_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final RestClient restClient;
    private final String afadApiUrl;
    private final SourceStatusTracker statusTracker;

    public AfadApiClient(@Value("${afad.api.url:https://deprem.afad.gov.tr/apiv2/event/filter}") String afadApiUrl,
                         SourceStatusTracker statusTracker) {
        this.afadApiUrl = afadApiUrl;
        this.statusTracker = statusTracker;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public List<AfadRawEventDto> fetchEvents(Instant start, Instant end) {
        Instant now = Instant.now();
        statusTracker.recordAttempt(now);

        String startStr = AFAD_DATE_FORMAT.format(start);
        String endStr = AFAD_DATE_FORMAT.format(end);

        log.info("Fetching AFAD events from '{}' to '{}'", startStr, endStr);

        try {
            List<AfadRawEventDto> events = restClient.get()
                    .uri(afadApiUrl + "?start={start}&end={end}", startStr, endStr)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AfadRawEventDto>>() {});

            if (events == null) {
                events = Collections.emptyList();
            }

            statusTracker.recordSuccess(now);
            log.info("Successfully fetched {} events from AFAD", events.size());
            return events;
        } catch (Exception e) {
            log.error("Failed to fetch events from AFAD: {}", e.getMessage());
            statusTracker.recordFailure(now, e.getMessage());
            return Collections.emptyList();
        }
    }
}
