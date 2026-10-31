package com.course.catalog.api;

import com.course.catalog.service.CatalogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Course catalog")
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {

    private final CatalogQueryService catalog;

    public CatalogController(CatalogQueryService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/offerings")
    @Operation(summary = "List the seeded course offering")
    public List<OfferingResponse> offerings() {
        return catalog.listOfferings();
    }

    @GetMapping("/offerings/{offeringId}")
    @Operation(summary = "Read one offering, including its prerequisite and capacity")
    public OfferingResponse offering(@PathVariable String offeringId) {
        return catalog.getOffering(offeringId);
    }

    @GetMapping("/learners/{learnerId}")
    @Operation(summary = "Read a learner and the course codes they have completed")
    public LearnerResponse learner(@PathVariable String learnerId) {
        return catalog.getLearner(learnerId);
    }
}
