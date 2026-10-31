package com.course.enrollment.client;

import java.util.Set;

public record LearnerSnapshot(String learnerId, String displayName, Set<String> completedCourseCodes) {
}
