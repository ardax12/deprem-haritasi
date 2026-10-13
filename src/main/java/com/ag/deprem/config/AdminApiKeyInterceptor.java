package com.ag.deprem.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AdminApiKeyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminApiKeyInterceptor.class);
    public static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    private final String configuredAdminKey;

    public AdminApiKeyInterceptor(@Value("${app.admin.api-key:}") String configuredAdminKey) {
        this.configuredAdminKey = configuredAdminKey != null ? configuredAdminKey.trim() : "";
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Skip pre-flight CORS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String providedKey = request.getHeader(ADMIN_KEY_HEADER);

        if (configuredAdminKey.isEmpty()) {
            log.warn("Blocked administrative request to '{}': APP_ADMIN_API_KEY is not configured", request.getRequestURI());
            sendErrorResponse(response, HttpStatus.SERVICE_UNAVAILABLE, "Yönetici API anahtarı sistemde yapılandırılmamış. İşlem reddedildi.");
            return false;
        }

        if (providedKey == null || providedKey.isBlank()) {
            log.warn("Unauthorized administrative request to '{}': missing X-Admin-Key header", request.getRequestURI());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "X-Admin-Key başlığı eksik.");
            return false;
        }

        byte[] expectedBytes = configuredAdminKey.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = providedKey.trim().getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
            log.warn("Forbidden administrative request to '{}': invalid X-Admin-Key", request.getRequestURI());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Geçersiz X-Admin-Key anahtarı.");
            return false;
        }

        return true;
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = String.format("{\"type\":\"urn:deprem:auth-error\",\"title\":\"Yetkilendirme Hatası\",\"status\":%d,\"detail\":\"%s\"}",
                status.value(), message);
        response.getWriter().write(json);
    }
}
