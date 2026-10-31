package com.course.enrollment.client;

public interface CatalogGateway {

    OfferingSnapshot getOffering(String offeringId);

    LearnerSnapshot getLearner(String learnerId);
}
