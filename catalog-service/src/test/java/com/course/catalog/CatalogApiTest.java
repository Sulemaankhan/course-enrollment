package com.course.catalog;

import com.course.common.DemoCourse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTheSeededOfferingAndPrerequisite() throws Exception {
        mockMvc.perform(get("/api/catalog/offerings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].offeringId").value(DemoCourse.OFFERING_ID))
                .andExpect(jsonPath("$[0].capacity").value(DemoCourse.CAPACITY))
                .andExpect(jsonPath("$[0].prerequisiteCourseCode").value(DemoCourse.PREREQUISITE));

        mockMvc.perform(get("/api/catalog/offerings/" + DemoCourse.OFFERING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(DemoCourse.TITLE));
    }

    @Test
    void distinguishesEligibleAndIneligibleLearners() throws Exception {
        mockMvc.perform(get("/api/catalog/learners/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice Chen"))
                .andExpect(jsonPath("$.completedCourseCodes", containsInAnyOrder(DemoCourse.PREREQUISITE)));

        mockMvc.perform(get("/api/catalog/learners/dan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCourseCodes", hasSize(0)));
    }

    @Test
    void unknownOfferingAndLearnerReturn404() throws Exception {
        mockMvc.perform(get("/api/catalog/offerings/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.outcome").value("OFFERING_NOT_FOUND"));
        mockMvc.perform(get("/api/catalog/learners/ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.outcome").value("LEARNER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Learner ghost was not found."));
    }

    @Test
    void unknownRoutesAndUnsupportedMethodsUseProblemDetails() throws Exception {
        mockMvc.perform(get("/api/catalog/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.outcome").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("No endpoint matches this request."))
                .andExpect(jsonPath("$.message").value("No endpoint matches this request."));
        mockMvc.perform(post("/api/catalog/offerings"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.outcome").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.status").value(405));
    }
}
