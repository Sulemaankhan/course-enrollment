package com.course.catalog.api;

public class UnknownCatalogResourceException extends RuntimeException {

    private final String outcome;

    public UnknownCatalogResourceException(String outcome, String message) {
        super(message);
        this.outcome = outcome;
    }

    public String getOutcome() {
        return outcome;
    }
}
