package com.course.enrollment.domain;

/**
 * Fixed eligibility rule for this slice. This is not a policy engine.
 * A learner is eligible when the offering has no prerequisite, or when the
 * learner's completed course codes contain that exact prerequisite code.
 */
public final class PrerequisiteRule {

    public boolean eligible(String prerequisiteCourseCode, java.util.Set<String> completedCourseCodes) {
        if (prerequisiteCourseCode == null || prerequisiteCourseCode.isBlank()) {
            return true;
        }
        return completedCourseCodes != null && completedCourseCodes.contains(prerequisiteCourseCode);
    }
}
