package com.course.catalog.service;

import com.course.catalog.api.LearnerResponse;
import com.course.catalog.api.OfferingResponse;
import com.course.catalog.api.UnknownCatalogResourceException;
import com.course.catalog.domain.CourseOffering;
import com.course.catalog.domain.Learner;
import com.course.catalog.persistence.CourseOfferingRepository;
import com.course.catalog.persistence.LearnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CatalogQueryService {

    private final CourseOfferingRepository offerings;
    private final LearnerRepository learners;

    public CatalogQueryService(CourseOfferingRepository offerings, LearnerRepository learners) {
        this.offerings = offerings;
        this.learners = learners;
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> listOfferings() {
        return offerings.findAll().stream().map(CatalogQueryService::toOffering).toList();
    }

    @Transactional(readOnly = true)
    public OfferingResponse getOffering(String offeringId) {
        return offerings.findById(offeringId)
                .map(CatalogQueryService::toOffering)
                .orElseThrow(() -> new UnknownCatalogResourceException(
                        "OFFERING_NOT_FOUND", "Offering " + offeringId + " was not found."));
    }

    @Transactional(readOnly = true)
    public LearnerResponse getLearner(String learnerId) {
        return learners.findById(learnerId)
                .map(CatalogQueryService::toLearner)
                .orElseThrow(() -> new UnknownCatalogResourceException(
                        "LEARNER_NOT_FOUND", "Learner " + learnerId + " was not found."));
    }

    private static OfferingResponse toOffering(CourseOffering offering) {
        return new OfferingResponse(
                offering.getOfferingId(),
                offering.getCourseCode(),
                offering.getTitle(),
                offering.getTerm(),
                offering.getCapacity(),
                offering.getPrerequisiteCourseCode(),
                offering.getDescription());
    }

    private static LearnerResponse toLearner(Learner learner) {
        return new LearnerResponse(
                learner.getLearnerId(),
                learner.getDisplayName(),
                Set.copyOf(learner.getCompletedCourseCodes()));
    }
}
