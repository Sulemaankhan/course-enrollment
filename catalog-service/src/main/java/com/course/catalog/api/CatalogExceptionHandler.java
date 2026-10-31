package com.course.catalog.api;

import com.course.common.web.ApiProblems;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CatalogExceptionHandler {

    @ExceptionHandler(UnknownCatalogResourceException.class)
    public ResponseEntity<ProblemDetail> missing(UnknownCatalogResourceException exception) {
        ProblemDetail problem = ApiProblems.of(HttpStatus.NOT_FOUND, exception.getOutcome(), exception.getMessage());
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
