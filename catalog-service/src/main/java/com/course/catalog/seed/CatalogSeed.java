package com.course.catalog.seed;

import com.course.catalog.domain.CourseOffering;
import com.course.catalog.domain.Learner;
import com.course.catalog.persistence.CourseOfferingRepository;
import com.course.catalog.persistence.LearnerRepository;
import com.course.common.DemoCourse;
import com.course.common.DemoLearners;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class CatalogSeed implements ApplicationRunner {

    private final CourseOfferingRepository offerings;
    private final LearnerRepository learners;

    public CatalogSeed(CourseOfferingRepository offerings, LearnerRepository learners) {
        this.offerings = offerings;
        this.learners = learners;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (offerings.count() == 0) {
            offerings.save(new CourseOffering(
                    DemoCourse.OFFERING_ID,
                    DemoCourse.COURSE_CODE,
                    DemoCourse.TITLE,
                    DemoCourse.TERM,
                    DemoCourse.CAPACITY,
                    DemoCourse.PREREQUISITE,
                    DemoCourse.DESCRIPTION));
        }
        if (learners.count() == 0) {
            for (DemoLearners.Person person : DemoLearners.ALL) {
                Set<String> completed = new HashSet<>();
                if (person.completedPrerequisite()) {
                    completed.add(DemoCourse.PREREQUISITE);
                }
                learners.save(new Learner(person.learnerId(), person.displayName(), completed));
            }
        }
    }
}
