package com.course.enrollment;

import com.course.common.DemoCourse;
import com.course.enrollment.client.CatalogUnavailableException;
import com.course.enrollment.client.InvalidRequestException;
import com.course.enrollment.api.EnrollmentController;
import com.course.enrollment.persistence.WaitlistEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnrollmentFlowTest extends EnrollmentTestBase {

    @Autowired
    private EnrollmentController controller;

    @Test
    void enrollsWhenCapacityRemainsAndPrerequisiteIsMet() throws Exception {
        enroll("alice")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("ENROLLED"))
                .andExpect(jsonPath("$.seatsRemaining").value(1));

        mockMvc.perform(get("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/status").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewer.state").value("ENROLLED"))
                .andExpect(jsonPath("$.viewer.eligible").value(true))
                .andExpect(jsonPath("$.eligibilityPolicy").value(org.hamcrest.Matchers.containsString("SKIP_INELIGIBLE")))
                .andExpect(jsonPath("$.roster[0].learnerId").value("alice"))
                .andExpect(jsonPath("$.roster[0].displayName").value("Alice Chen"));
    }

    @Test
    void fullCourseWaitlistsOnceAndDuplicateRequestsDoNotCreateRecords() throws Exception {
        enroll("alice").andExpect(status().isCreated());
        enroll("bob").andExpect(status().isCreated());
        enroll("carol")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.outcome").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(1));
        enroll("carol")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALREADY_WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(1));
        enroll("alice")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALREADY_ENROLLED"));

        assertEquals(2, roster.count());
        assertEquals(1, waitlist.count());
    }

    @Test
    void dropPromotesTheEarliestEligibleLearnerAndASecondDropDoesNotPromoteAgain() throws Exception {
        enroll("alice").andExpect(status().isCreated());
        enroll("bob").andExpect(status().isCreated());
        enroll("carol").andExpect(status().isAccepted());
        enroll("eve").andExpect(jsonPath("$.waitlistPosition").value(2));

        drop("alice")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("DROPPED"))
                .andExpect(jsonPath("$.promotedLearnerId").value("carol"));

        mockMvc.perform(get("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/status").with(user("eve")))
                .andExpect(jsonPath("$.viewer.state").value("WAITLISTED"))
                .andExpect(jsonPath("$.viewer.waitlistPosition").value(1))
                .andExpect(jsonPath("$.roster[0].learnerId").value("bob"))
                .andExpect(jsonPath("$.roster[1].learnerId").value("carol"));

        drop("alice")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.outcome").value("NOT_ENROLLED"));

        assertEquals(2, roster.count());
        assertEquals(1, waitlist.count());
        assertTrue(waitlist.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "eve").isPresent());
        assertTrue(roster.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "bob").isPresent());
        assertTrue(roster.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "carol").isPresent());
    }

    @Test
    void ineligibleLearnerIsNotEnrolledOrWaitlisted() throws Exception {
        enroll("dan")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.outcome").value("PREREQUISITE_NOT_MET"));
        assertEquals(0, roster.count());
        assertEquals(0, waitlist.count());

        enroll("alice");
        enroll("bob");
        enroll("carol");
        enroll("dan").andExpect(status().isUnprocessableEntity());
        assertEquals(1, waitlist.count());
        assertTrue(waitlist.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "carol").isPresent());
        assertFalse(waitlist.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "dan").isPresent());
    }

    @Test
    void promotionSkipsAnIneligibleWaitlistedLearnerAndDoesNotKeepThem() throws Exception {
        catalog.setCapacity(1);
        catalog.setCompleted("dan", Set.of(DemoCourse.PREREQUISITE));
        enroll("alice").andExpect(status().isCreated());
        enroll("dan").andExpect(status().isAccepted());
        enroll("eve").andExpect(jsonPath("$.waitlistPosition").value(2));
        catalog.setCompleted("dan", Set.of());

        mockMvc.perform(get("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/status").with(user("dan")))
                .andExpect(jsonPath("$.waitlist[0].learnerId").value("dan"))
                .andExpect(jsonPath("$.waitlist[0].eligible").value(false))
                .andExpect(jsonPath("$.viewer.state").value("WAITLISTED"));

        drop("alice")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promotedLearnerId").value("eve"))
                .andExpect(jsonPath("$.skippedLearnerIds[0]").value("dan"));

        assertFalse(roster.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "dan").isPresent());
        assertFalse(waitlist.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "dan").isPresent());
        assertTrue(roster.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "eve").isPresent());
        assertEquals(0, waitlist.count());
    }

    @Test
    void newLearnerDoesNotJumpAheadOfTheWaitlist() throws Exception {
        catalog.setCapacity(1);
        waitlist.saveAndFlush(new WaitlistEntry(
                DemoCourse.OFFERING_ID, "bob", "Bob Patel", clock.instant()));

        enroll("carol")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.promotedLearnerId").value("bob"))
                .andExpect(jsonPath("$.waitlistPosition").value(1));

        assertTrue(roster.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "bob").isPresent());
        assertTrue(waitlist.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "carol").isPresent());
    }

    @Test
    void leavingTheWaitlistDoesNotPromoteAnyone() throws Exception {
        catalog.setCapacity(1);
        enroll("alice").andExpect(status().isCreated());
        enroll("bob").andExpect(status().isAccepted());

        drop("bob")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("LEFT_WAITLIST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("nobody was promoted")));
        drop("bob").andExpect(jsonPath("$.outcome").value("NOT_ENROLLED"));

        assertTrue(roster.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "alice").isPresent());
        assertEquals(0, waitlist.count());
        assertEquals(1, roster.count());
    }

    @Test
    void unknownOfferingLearnerAndMissingAuthenticationDoNotChangeState() throws Exception {
        mockMvc.perform(post("/api/enrollments/offerings/NOPE/enroll").with(user("alice")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.outcome").value("OFFERING_NOT_FOUND"));
        mockMvc.perform(post("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/enroll").with(user("ghost")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.outcome").value("LEARNER_NOT_FOUND"));
        mockMvc.perform(post("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/drop"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.outcome").value("INVALID_REQUEST"));
        drop("dan").andExpect(jsonPath("$.outcome").value("NOT_ENROLLED"));

        assertEquals(0, roster.count());
        assertEquals(0, waitlist.count());
    }

    @Test
    void idempotencyKeyReplaysTheOriginalResultAndRejectsAMalformedKey() throws Exception {
        enrollWithKey("alice", "enroll-alice-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("ENROLLED"));
        enrollWithKey("alice", "enroll-alice-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("ENROLLED"));
        assertEquals(1, roster.count());

        mockMvc.perform(post("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/enroll")
                        .with(user("bob"))
                        .header("Idempotency-Key", "bad key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.outcome").value("INVALID_REQUEST"));
        mockMvc.perform(get("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/enroll").with(user("bob")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.outcome").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(get("/api/does-not-exist").with(user("bob")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.outcome").value("NOT_FOUND"));
        assertFalse(roster.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "bob").isPresent());
    }

    @Test
    void catalogOutageAndCacheFailureDoNotCorruptState() throws Exception {
        catalog.setCapacity(1);
        enroll("alice").andExpect(status().isCreated());

        queue.setFail(true);
        enroll("bob")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.waitlistPosition").value(1));
        assertEquals(1, roster.count());
        assertEquals(1, waitlist.count());
        queue.setFail(false);

        catalog.failWith(new CatalogUnavailableException("down"));
        enroll("carol").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.outcome").value("CATALOG_UNAVAILABLE"));
        assertEquals(1, waitlist.count());
    }

    @Test
    void promotionFailureRollsBackTheDrop() throws Exception {
        catalog.setCapacity(1);
        enroll("alice");
        enroll("carol");
        catalog.succeedTimes(2);

        drop("alice").andExpect(status().isServiceUnavailable());

        assertTrue(roster.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "alice").isPresent());
        assertTrue(waitlist.findByOfferingIdAndLearnerId(DemoCourse.OFFERING_ID, "carol").isPresent());
        assertEquals(1, roster.count());
        assertEquals(1, waitlist.count());
    }

    @Test
    void registrarOverviewReadsTheBoardWithoutEnrolling() throws Exception {
        enroll("alice").andExpect(status().isCreated());

        mockMvc.perform(get("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/overview").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewer.learnerId").value("admin"))
                .andExpect(jsonPath("$.viewer.displayName").value("admin"))
                .andExpect(jsonPath("$.viewer.state").value("NONE"))
                .andExpect(jsonPath("$.viewer.eligible").value(false))
                .andExpect(jsonPath("$.roster[0].learnerId").value("alice"))
                .andExpect(jsonPath("$.enrolledCount").value(1));

        assertEquals(1, roster.count());
        assertEquals(0, waitlist.count());
    }

    @Test
    void eventSubscriptionDoesNotChangeEnrollment() {
        controller.events(DemoCourse.OFFERING_ID);
        assertEquals(0, roster.count());
        assertEquals(0, waitlist.count());
    }

    @Test
    void rejectsAnOversizedOfferingIdBeforeTouchingTheCatalog() {
        assertThrows(InvalidRequestException.class, () -> service.enroll("x".repeat(41), "alice", null));
        assertEquals(0, roster.count());
    }
}
