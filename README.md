# Course enrollment and FIFO waitlist

One course offering. A learner enrolls once, joins a FIFO waitlist when the section is full, and the earliest eligible learner is promoted when a seat opens.

## Eligibility policy: SKIP_INELIGIBLE

This slice does not have a configurable policy engine.

- The offering has one prerequisite course code, `CS-201`. A learner is eligible when that code is in their completed-course set. There is no grade, expiry, or "in progress" state.
- A learner holds at most one roster seat and one waitlist place. After dropping, they may request enrollment again.
- An ineligible learner is not enrolled and is not added to the waitlist.
- When a seat opens, the waitlist is walked in FIFO order (`enqueuedAt`, then `learnerId`). Ineligible learners are removed and do not keep their place. The earliest remaining eligible learner is promoted.
- A duplicate enroll or drop does not create another row. Leaving the waitlist does not free a seat and does not promote anyone.
- A status read does not change the queue. Someone who becomes ineligible while waiting stays visible, marked ineligible, until the next drop or a new eligible enrollment runs promotion.
- A new eligible learner cannot pass people already waiting. Open seats are filled from the front of the queue first.

The seeded offering is `CS-301-2026FA`, Data Structures, capacity 2, so the waitlist is easy to see. Dan Brooks has not completed CS-201. Alice, Bob, Carol, and Eve have.

## Services

| Process | Port | Role |
| --- | --- | --- |
| React desk | 5173 | Sign in, learner enrollment screen, registrar screen |
| API gateway | 8080 | JWT login, routing, circuit breaker, CORS |
| Catalog | 8081 | Offering metadata, prerequisite, capacity, completed courses |
| Enrollment | 8082 | Roster, waitlist, promotion, Swagger |

The local waitlist is an in-memory FIFO queue with the same order as the database (`enqueuedAt`, then `learnerId`). The database is the durable copy. Promotion locks the offering row, so the last seat cannot be given out twice. Nothing here needs Docker, Redis, or Oracle. H2 files live under `data/`.

## Run

Use JDK 17. Maven on this machine may still point at JDK 8, so set `JAVA_HOME` first.

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
mvn test
```

Coverage for the enrollment service is enforced at 90% line coverage:

```powershell
mvn -pl enrollment-service verify
```

The report is `enrollment-service/target/site/jacoco/index.html`.

Start the three services and the UI. No Docker. `-am` builds `common` in the same reactor. `common` is a shared library, not a process, so Spring Boot does not launch it.

```powershell
mvn -pl catalog-service -am spring-boot:run
mvn -pl enrollment-service -am spring-boot:run
mvn -pl gateway -am spring-boot:run
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and sign in. The demo password for every account is `password`.

Learners (`alice`, `bob`, `carol`, `dan`, `eve`) open the enrollment screen and can enroll or drop only as themselves. The registrar account `admin` opens the roster and waitlist and cannot take or leave a seat. A learner token is rejected from the registrar overview, and the registrar token is rejected from enroll and drop.

Swagger is at http://localhost:8081/swagger-ui.html and http://localhost:8082/swagger-ui.html. Override the JWT secret with `JWT_SECRET` before any shared deployment. The default secret is for local use only.
