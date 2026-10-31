package com.course.enrollment.seed;

import com.course.common.DemoCourse;
import com.course.enrollment.client.CatalogGateway;
import com.course.enrollment.client.OfferingSnapshot;
import com.course.enrollment.persistence.OfferingCapacity;
import com.course.enrollment.persistence.OfferingCapacityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CapacitySeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CapacitySeed.class);

    private final CatalogGateway catalog;
    private final OfferingCapacityRepository capacities;

    public CapacitySeed(CatalogGateway catalog, OfferingCapacityRepository capacities) {
        this.catalog = catalog;
        this.capacities = capacities;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            OfferingSnapshot offering = catalog.getOffering(DemoCourse.OFFERING_ID);
            if (!capacities.existsById(offering.offeringId())) {
                capacities.save(OfferingCapacity.from(offering));
                log.info("Seeded capacity for {}", offering.offeringId());
            }
        } catch (RuntimeException ex) {
            log.warn("Catalog not ready at startup. Capacity will be created on the first request. {}", ex.getMessage());
        }
    }
}
