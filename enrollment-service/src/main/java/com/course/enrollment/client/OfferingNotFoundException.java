package com.course.enrollment.client;

public class OfferingNotFoundException extends RuntimeException {

    private final String offeringId;

    public OfferingNotFoundException(String offeringId) {
        super("Offering " + offeringId + " was not found.");
        this.offeringId = offeringId;
    }

    public String getOfferingId() {
        return offeringId;
    }
}
