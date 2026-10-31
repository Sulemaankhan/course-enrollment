package com.course.enrollment;

import com.course.common.DemoCourse;
import com.course.common.JwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("securetest")
class SecurityEnabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${security.jwt.secret}")
    private String secret;

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.outcome").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void validTokenIsAuthenticatedAndCatalogFailureStaysClear() throws Exception {
        String token = JwtSupport.issue(secret, "alice", "Alice Chen");
        mockMvc.perform(get("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void learnerCanReachEnrollAndCannotOpenTheRegistrarOverview() throws Exception {
        String token = JwtSupport.issue(secret, "alice", "Alice Chen");
        mockMvc.perform(post("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/enroll")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.outcome").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have access to this resource."));
    }

    @Test
    void registrarCannotEnrollAndCanRequestTheOverview() throws Exception {
        String token = JwtSupport.issue(secret, "admin", "Registrar", "ADMIN");
        mockMvc.perform(post("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/enroll")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/enrollments/offerings/" + DemoCourse.OFFERING_ID + "/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable());
    }
}
