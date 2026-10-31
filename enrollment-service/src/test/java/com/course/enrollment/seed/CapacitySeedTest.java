package com.course.enrollment.seed;

import com.course.common.DemoCourse;
import com.course.enrollment.client.CatalogGateway;
import com.course.enrollment.client.CatalogUnavailableException;
import com.course.enrollment.client.OfferingSnapshot;
import com.course.enrollment.persistence.OfferingCapacity;
import com.course.enrollment.persistence.OfferingCapacityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapacitySeedTest {

    @Test
    void seedsOnceAndIgnoresACatalogOutage() {
        CatalogGateway catalog = mock(CatalogGateway.class);
        OfferingCapacityRepository capacities = mock(OfferingCapacityRepository.class);
        CapacitySeed seed = new CapacitySeed(catalog, capacities);
        OfferingSnapshot snapshot = new OfferingSnapshot(
                DemoCourse.OFFERING_ID, DemoCourse.COURSE_CODE, DemoCourse.TITLE, DemoCourse.TERM,
                DemoCourse.CAPACITY, DemoCourse.PREREQUISITE);
        when(catalog.getOffering(DemoCourse.OFFERING_ID)).thenReturn(snapshot);
        when(capacities.existsById(DemoCourse.OFFERING_ID)).thenReturn(false, true);

        seed.run(mock(ApplicationArguments.class));
        seed.run(mock(ApplicationArguments.class));
        verify(capacities, times(1)).save(any(OfferingCapacity.class));

        when(catalog.getOffering(DemoCourse.OFFERING_ID)).thenThrow(new CatalogUnavailableException("down"));
        seed.run(mock(ApplicationArguments.class));
        verify(capacities, never()).deleteAll();
    }
}
