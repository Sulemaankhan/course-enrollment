package com.course.common;

import java.util.List;

public final class DemoLearners {

    public record Person(String learnerId, String displayName, boolean completedPrerequisite) {
    }

    /** Shared local-demo password. The gateway is the only component that checks it. */
    public static final String DEMO_PASSWORD = "password";

    public static final List<Person> ALL = List.of(
            new Person("alice", "Alice Chen", true),
            new Person("bob", "Bob Patel", true),
            new Person("carol", "Carol Diaz", true),
            new Person("dan", "Dan Brooks", false),
            new Person("eve", "Eve Nguyen", true)
    );

    private DemoLearners() {
    }

    public static Person find(String learnerId) {
        return ALL.stream()
                .filter(person -> person.learnerId().equals(learnerId))
                .findFirst()
                .orElse(null);
    }
}
