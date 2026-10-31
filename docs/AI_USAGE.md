# AI usage log

The implementation in this repository was produced with Cursor's coding agent (Grok 4.7). The notes below are the engineering decisions from that session: the prompt, what was kept, what was changed, and how it was checked.

## 1. Slice design from the problem statement and architecture sketch

Prompt, summarized: implement enrollment, drop, and FIFO waitlist promotion for one offering; state whether an ineligible learner is skipped or stays waiting; cover capacity, duplicates, FIFO promotion, prerequisite failure, and invalid drops with tests; use Java 17, Spring Boot 3.5.16, React, a gateway and circuit breaker, Oracle, Swagger, Spring Security, Redis as the FIFO cache, and about 90% coverage. Do not build payments, identity integration, or a general registration product.

Response used: a four-part layout (React desk, Spring Cloud Gateway, catalog service, enrollment service) matching the sketch. Enrollment locks one offering row, checks the single prerequisite, and either writes one roster row or one waitlist row. Redis is a sorted set rewritten from the database order after each change.

Accepted: skip-ineligible policy, FIFO by enqueue time, idempotency so a repeated enroll does not insert a second row, pessimistic locking for the last seat, circuit breaker around the catalog client and on the gateway routes.

Edited: the sketch labels the database PostgreSQL. The requested database is Oracle, so production configuration uses Oracle JDBC and `OracleDialect`. Tests and the default local run use H2, because an Oracle container is optional and the acceptance tests must run without it. Redis is a cache, not the only copy of the queue, so a Redis outage does not roll back a committed enroll or drop.

Rejected: a configurable policy engine, a second offering, payments, and a separate identity provider. Login is a local demo user list and an HMAC JWT.

## 2. Promotion behavior

Prompt, in effect: when a seat opens, promote the earliest eligible learner, and apply the skip-or-retain choice consistently.

Response used: on a drop, and before a new eligible learner takes a seat, remove waitlisted learners who currently fail the prerequisite, then enroll from the front of the remaining queue until the open seats are filled.

Accepted: ineligible learners are not inserted, and once a promotion pass considers them they are deleted rather than left in place. The earliest eligible learner is the one promoted into the freed seat.

Edited: a status GET does not purge the queue. An ineligible waiter stays visible and is flagged `eligible: false` until the next drop or new enrollment. That keeps reads free of writes. A duplicate enroll or a waitlist leave does not promote anyone else.

Verified by `EnrollmentFlowTest` (open seat, full section, duplicate enroll, FIFO promotion, prerequisite rejection, skip-on-promotion, queue jumping, invalid and repeated drops, catalog failure rollback) and `ConcurrentEnrollmentTest` (one winner for the last seat, one row for concurrent duplicate enrolls).

## 3. What was checked

Commands run after the code was written:

- `mvn test` with `JAVA_HOME` pointed at JDK 17
- JaCoCo check on `enrollment-service` (`mvn -pl enrollment-service verify`), minimum 0.90 line coverage
- UI flow in the browser: sign in, enroll until full, waitlist, reject the ineligible learner, drop, and confirm FIFO promotion

Results of those commands are in the session that produced this file. If a check failed, the code was changed and the check was run again.
