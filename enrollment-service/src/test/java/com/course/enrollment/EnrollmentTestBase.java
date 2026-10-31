package com.course.enrollment;

import com.course.common.DemoCourse;
import com.course.enrollment.persistence.IdempotencyRepository;
import com.course.enrollment.persistence.OfferingCapacityRepository;
import com.course.enrollment.persistence.RosterRepository;
import com.course.enrollment.persistence.WaitlistRepository;
import com.course.enrollment.service.EnrollmentService;
import com.course.enrollment.support.FakeCatalogGateway;
import com.course.enrollment.support.MutableClock;
import com.course.enrollment.support.TestInfrastructure;
import com.course.enrollment.support.UnstableQueue;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestInfrastructure.class)
abstract class EnrollmentTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected FakeCatalogGateway catalog;

    @Autowired
    protected UnstableQueue queue;

    @Autowired
    protected RosterRepository roster;

    @Autowired
    protected WaitlistRepository waitlist;

    @Autowired
    protected OfferingCapacityRepository capacities;

    @Autowired
    protected IdempotencyRepository idempotency;

    @Autowired
    protected EnrollmentService service;

    @BeforeEach
    void resetState() {
        idempotency.deleteAll();
        waitlist.deleteAll();
        roster.deleteAll();
        capacities.deleteAll();
        catalog.reset();
        queue.reset();
        clock.reset();
    }

    protected ResultActions enroll(String learner) throws Exception {
        clock.advanceMillis(5);
        return mockMvc.perform(post("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/enroll")
                .with(user(learner)));
    }

    protected ResultActions enrollWithKey(String learner, String key) throws Exception {
        clock.advanceMillis(5);
        return mockMvc.perform(post("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/enroll")
                .with(user(learner))
                .header("Idempotency-Key", key));
    }

    protected ResultActions drop(String learner) throws Exception {
        return mockMvc.perform(post("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/drop")
                .with(user(learner)));
    }
}
