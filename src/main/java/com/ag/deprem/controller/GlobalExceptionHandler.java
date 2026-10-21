package com.ag.deprem.controller;

import com.ag.deprem.service.AfadApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request parameter: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Geçersiz Parametre");
        problem.setType(URI.create("urn:deprem:bad-request"));
        return problem;
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Doğrulama hatası");
        log.warn("Validation failure: {}", detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Geçersiz Veri Girişi");
        problem.setType(URI.create("urn:deprem:bad-request-validation"));
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException e) {
        log.warn("Illegal state conflict: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("İşlem Reddedildi");
        problem.setType(URI.create("urn:deprem:conflict"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String detail = String.format("'%s' parametresi için geçersiz değer: '%s'", e.getName(), e.getValue());
        log.warn("Parameter type mismatch: {}", detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Parametre Tipi Uyuşmazlığı");
        problem.setType(URI.create("urn:deprem:bad-request-type"));
        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException e) {
        String detail = String.format("Zorunlu parametre eksik: '%s'", e.getParameterName());
        log.warn("Missing parameter: {}", detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Eksik Parametre");
        problem.setType(URI.create("urn:deprem:missing-parameter"));
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("Malformed HTTP request body: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "İstek gövdesi okunamadı veya biçimi hatalı.");
        problem.setTitle("Hatalı İstek Gövdesi");
        problem.setType(URI.create("urn:deprem:bad-request-body"));
        return problem;
    }

    @ExceptionHandler(SecurityException.class)
    public ProblemDetail handleSecurityException(SecurityException e) {
        log.warn("Security violation: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        problem.setTitle("Erişim Engellendi");
        problem.setType(URI.create("urn:deprem:forbidden"));
        return problem;
    }

    @ExceptionHandler(AfadApiClient.AfadApiException.class)
    public ProblemDetail handleAfadOutage(AfadApiClient.AfadApiException e) {
        log.error("AFAD service outage: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "AFAD veri kaynağına şu anda ulaşılamıyor. Lütfen daha sonra tekrar deneyin.");
        problem.setTitle("Kaynak Servis Kesintisi");
        problem.setType(URI.create("urn:deprem:upstream-outage"));
        return problem;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNotFound(NoSuchElementException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Kayıt Bulunamadı");
        problem.setType(URI.create("urn:deprem:not-found"));
        return problem;
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "İstenen kaynak bulunamadı: " + e.getResourcePath());
        problem.setTitle("Kaynak Bulunamadı");
        problem.setType(URI.create("urn:deprem:not-found"));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception e) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled error [correlationId={}]: {}", errorId, e.getMessage(), e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Beklenmeyen bir sunucu hatası oluştu. Takip kodu: " + errorId
        );
        problem.setTitle("Sunucu Hatası");
        problem.setType(URI.create("urn:deprem:internal-error"));
        problem.setProperty("correlationId", errorId);
        return problem;
    }
}
