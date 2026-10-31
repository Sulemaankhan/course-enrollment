package com.course.catalog.persistence;

import com.course.catalog.domain.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, String> {
}
