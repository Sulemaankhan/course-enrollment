package com.course.enrollment.api;

import com.course.enrollment.domain.Outcome;

import java.util.List;

public record CommandResult(
        Outcome outcome,
        String offeringId,
        String learnerId,
        String message,
        Integer waitlistPosition,
        Integer seatsRemaining,
        String promotedLearnerId,
        List<String> skippedLearnerIds
) {
    public CommandResult {
        skippedLearnerIds = skippedLearnerIds == null ? List.of() : List.copyOf(skippedLearnerIds);
    }

    public static CommandResult of(Outcome outcome, String offeringId, String learnerId, String message) {
        return new CommandResult(outcome, offeringId, learnerId, message, null, null, null, List.of());
    }
}
