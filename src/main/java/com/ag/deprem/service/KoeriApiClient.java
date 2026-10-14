package com.ag.deprem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;

@Service
public class KoeriApiClient {

    public static class KoeriApiException extends RuntimeException {
        public KoeriApiException(String message) {
            super(message);
        }
        public KoeriApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(KoeriApiClient.class);
    private static final Charset WINDOWS_1254 = Charset.forName("windows-1254");
    private static final int MAX_RETRIES = 3;

    private final RestClient restClient;
    private final String koeriPageUrl;
    private final SourceStatusTracker statusTracker;

    public KoeriApiClient(@Value("${koeri.page.url:http://www.koeri.boun.edu.tr/scripts/lst0.asp}") String koeriPageUrl,
                          @Value("${koeri.http.connect-timeout-ms:10000}") int connectTimeoutMs,
                          @Value("${koeri.http.read-timeout-ms:20000}") int readTimeoutMs,
                          SourceStatusTracker statusTracker) {
        this.koeriPageUrl = koeriPageUrl;
        this.statusTracker = statusTracker;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; DepremIzleme/2026; +https://deprem.app)")
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7")
                .build();
    }

    public String fetchRawPage() {
        Instant now = Instant.now();
        statusTracker.recordAttempt("KOERI", now);

        log.info("Fetching KOERI recent earthquakes from '{}'", koeriPageUrl);

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                byte[] responseBytes = restClient.get()
                        .uri(koeriPageUrl)
                        .retrieve()
                        .body(byte[].class);

                if (responseBytes == null || responseBytes.length == 0) {
                    throw new IllegalStateException("Empty response body from KOERI");
                }

                String pageContent = new String(responseBytes, WINDOWS_1254);
                statusTracker.recordSuccess("KOERI", now);
                log.info("Successfully fetched KOERI page ({} bytes, attempt {})", responseBytes.length, attempt);
                return pageContent;
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed to fetch from KOERI: {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(attempt * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        String errorMsg = lastException != null ? lastException.getMessage() : "Unknown KOERI error";
        statusTracker.recordFailure("KOERI", now, errorMsg);
        log.error("All {} attempts to fetch from KOERI failed: {}", MAX_RETRIES, errorMsg);
        throw new KoeriApiException("KOERI veri alım hatası: " + errorMsg, lastException);
    }
}
