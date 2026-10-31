package com.course.enrollment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    Optional<WaitlistEntry> findByOfferingIdAndLearnerId(String offeringId, String learnerId);

    List<WaitlistEntry> findByOfferingIdOrderByEnqueuedAtAscLearnerIdAsc(String offeringId);

    int countByOfferingId(String offeringId);
}
