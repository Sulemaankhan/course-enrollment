package com.course.enrollment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RosterRepository extends JpaRepository<RosterEntry, Long> {

    Optional<RosterEntry> findByOfferingIdAndLearnerId(String offeringId, String learnerId);

    List<RosterEntry> findByOfferingIdOrderByEnrolledAtAscLearnerIdAsc(String offeringId);

    int countByOfferingId(String offeringId);
}
