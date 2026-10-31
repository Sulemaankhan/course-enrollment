package com.course.catalog.api;

import java.util.Set;

public record LearnerResponse(String learnerId, String displayName, Set<String> completedCourseCodes) {
}
