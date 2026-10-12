package com.ag.deprem.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNotFound(NoSuchElementException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Kayıt Bulunamadı");
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
