package com.course.enrollment.client;

public class LearnerNotFoundException extends RuntimeException {

    private final String learnerId;

    public LearnerNotFoundException(String learnerId) {
        super("Learner " + learnerId + " was not found.");
        this.learnerId = learnerId;
    }

    public String getLearnerId() {
        return learnerId;
    }
}
