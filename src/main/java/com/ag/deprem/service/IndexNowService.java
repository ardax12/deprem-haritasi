package com.ag.deprem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class IndexNowService {

    private static final Logger log = LoggerFactory.getLogger(IndexNowService.class);
    private static final String INDEXNOW_API_ENDPOINT = "https://api.indexnow.org/indexnow";

    @Value("${app.site.base-url:https://depremizleme.com}")
    private String baseUrl;

    @Value("${app.seo.indexnow.enabled:true}")
    private boolean enabled;

    @Value("${app.seo.indexnow.key:depremizleme2026indexnowkey}")
    private String indexNowKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String getIndexNowKey() {
        return indexNowKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Asynchronously submits updated URLs to the IndexNow network (Bing, Yandex, Seznam, etc.)
     */
    public CompletableFuture<Boolean> submitUrls(List<String> relativeOrAbsoluteUrls) {
        if (!enabled || relativeOrAbsoluteUrls == null || relativeOrAbsoluteUrls.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String cleanBase = baseUrl.replaceAll("/+$", "");
                String host = URI.create(cleanBase).getHost();
                if (host == null || host.isBlank()) {
                    host = "depremizleme.com";
                }

                List<String> fullUrls = relativeOrAbsoluteUrls.stream()
                        .map(u -> u.startsWith("http") ? u : cleanBase + (u.startsWith("/") ? u : "/" + u))
                        .toList();

                StringBuilder urlListJson = new StringBuilder();
                for (int i = 0; i < fullUrls.size(); i++) {
                    urlListJson.append("\"").append(escapeJson(fullUrls.get(i))).append("\"");
                    if (i < fullUrls.size() - 1) {
                        urlListJson.append(",");
                    }
                }

                String payload = "{\"host\":\"" + escapeJson(host) + "\","
                        + "\"key\":\"" + escapeJson(indexNowKey) + "\","
                        + "\"keyLocation\":\"" + escapeJson(cleanBase + "/" + indexNowKey + ".txt") + "\","
                        + "\"urlList\":[" + urlListJson + "]}";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(INDEXNOW_API_ENDPOINT))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode == 200 || statusCode == 202) {
                    log.info("IndexNow submitted successfully {} URLs (status: {})", relativeOrAbsoluteUrls.size(), statusCode);
                    return true;
                } else {
                    log.warn("IndexNow response code {} with body: {}", statusCode, response.body());
                    return false;
                }
            } catch (Exception e) {
                log.warn("Failed to submit URLs to IndexNow: {}", e.getMessage());
                return false;
            }
        });
    }
}
