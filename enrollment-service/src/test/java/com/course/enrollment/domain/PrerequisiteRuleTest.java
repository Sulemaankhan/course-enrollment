package com.course.enrollment.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrerequisiteRuleTest {

    private final PrerequisiteRule rule = new PrerequisiteRule();

    @Test
    void blankPrerequisiteIsOpenToEveryLearner() {
        assertTrue(rule.eligible(null, Set.of()));
        assertTrue(rule.eligible("  ", null));
    }

    @Test
    void requiresTheExactCompletedCourseCode() {
        assertTrue(rule.eligible("CS-201", Set.of("CS-101", "CS-201")));
        assertFalse(rule.eligible("CS-201", Set.of("CS-101")));
        assertFalse(rule.eligible("CS-201", null));
    }
}
