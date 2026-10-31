package com.course.catalog.persistence;

import com.course.catalog.domain.Learner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerRepository extends JpaRepository<Learner, String> {
}
