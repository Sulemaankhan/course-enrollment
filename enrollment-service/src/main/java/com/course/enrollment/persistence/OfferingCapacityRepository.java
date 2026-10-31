package com.course.enrollment.persistence;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OfferingCapacityRepository extends JpaRepository<OfferingCapacity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "8000"))
    @Query("select o from OfferingCapacity o where o.offeringId = :offeringId")
    Optional<OfferingCapacity> lockByOfferingId(@Param("offeringId") String offeringId);
}
