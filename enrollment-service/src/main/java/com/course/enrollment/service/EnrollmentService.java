package com.course.enrollment.service;

import com.course.enrollment.api.CommandResult;
import com.course.enrollment.api.StatusView;
import com.course.enrollment.client.CatalogGateway;
import com.course.enrollment.client.InvalidRequestException;
import com.course.enrollment.client.LearnerNotFoundException;
import com.course.enrollment.client.LearnerSnapshot;
import com.course.enrollment.client.OfferingSnapshot;
import com.course.enrollment.domain.EligibilityPolicy;
import com.course.enrollment.domain.Outcome;
import com.course.enrollment.domain.PrerequisiteRule;
import com.course.enrollment.persistence.IdempotencyRecord;
import com.course.enrollment.persistence.IdempotencyRepository;
import com.course.enrollment.persistence.OfferingCapacity;
import com.course.enrollment.persistence.OfferingCapacityRepository;
import com.course.enrollment.persistence.RosterEntry;
import com.course.enrollment.persistence.RosterRepository;
import com.course.enrollment.persistence.WaitlistEntry;
import com.course.enrollment.persistence.WaitlistRepository;
import com.course.enrollment.queue.FifoWaitlistQueue;
import com.course.enrollment.queue.QueuedLearner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Enrollment, drop, and FIFO promotion for one offering.
 * Ineligible learners are skipped: they are not enrolled and are not kept once a promotion pass runs.
 * The offering row is locked for the whole command so capacity cannot be oversold.
 */
@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._:-]{1,80}");

    private final CatalogGateway catalog;
    private final OfferingCapacityRepository capacities;
    private final RosterRepository roster;
    private final WaitlistRepository waitlist;
    private final IdempotencyRepository idempotency;
    private final FifoWaitlistQueue queue;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final PrerequisiteRule rule = new PrerequisiteRule();

    public EnrollmentService(CatalogGateway catalog, OfferingCapacityRepository capacities, RosterRepository roster,
                             WaitlistRepository waitlist, IdempotencyRepository idempotency, FifoWaitlistQueue queue,
                             ObjectMapper objectMapper, Clock clock) {
        this.catalog = catalog;
        this.capacities = capacities;
        this.roster = roster;
        this.waitlist = waitlist;
        this.idempotency = idempotency;
        this.queue = queue;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public CommandResult enroll(String offeringId, String learnerId, String idempotencyKey) {
        validateIdentity(offeringId, learnerId);
        String key = normalizeKey(idempotencyKey);
        Optional<CommandResult> replay = replay(offeringId, learnerId, "ENROLL", key);
        if (replay.isPresent()) {
            return replay.get();
        }

        OfferingSnapshot offering = catalog.getOffering(offeringId);
        LearnerSnapshot learner = catalog.getLearner(learnerId);
        OfferingCapacity locked = lockOrCreate(offering);
        Map<String, LearnerSnapshot> cache = new HashMap<>();
        cache.put(learner.learnerId(), learner);

        if (roster.findByOfferingIdAndLearnerId(offeringId, learnerId).isPresent()) {
            return remember(offeringId, learnerId, "ENROLL", key, new CommandResult(
                    Outcome.ALREADY_ENROLLED, offeringId, learnerId,
                    learner.displayName() + " is already enrolled. No duplicate roster record was created.",
                    null, seatsRemaining(locked), null, List.of()));
        }
        if (waitlist.findByOfferingIdAndLearnerId(offeringId, learnerId).isPresent()) {
            sync(offeringId);
            return remember(offeringId, learnerId, "ENROLL", key, new CommandResult(
                    Outcome.ALREADY_WAITLISTED, offeringId, learnerId,
                    learner.displayName() + " is already on the waitlist at position " + position(offeringId, learnerId)
                            + ". No duplicate waitlist record was created.",
                    position(offeringId, learnerId), seatsRemaining(locked), null, List.of()));
        }
        if (!isEligible(locked, learnerId, cache)) {
            return remember(offeringId, learnerId, "ENROLL", key, new CommandResult(
                    Outcome.PREREQUISITE_NOT_MET, offeringId, learnerId,
                    learner.displayName() + " has not completed prerequisite " + locked.getPrerequisiteCourseCode()
                            + ". Ineligible learners are skipped: they are not enrolled and are not added to the waitlist.",
                    null, seatsRemaining(locked), null, List.of()));
        }

        Promotion promotion = promoteUntilFull(locked, cache);
        if (roster.countByOfferingId(offeringId) < locked.getCapacity()) {
            roster.saveAndFlush(new RosterEntry(offeringId, learnerId, learner.displayName(), clock.instant()));
            sync(offeringId);
            int seats = seatsRemaining(locked);
            log.info("Enrolled {} in {}. Seats remaining {}", learnerId, offeringId, seats);
            return remember(offeringId, learnerId, "ENROLL", key, new CommandResult(
                    Outcome.ENROLLED, offeringId, learnerId,
                    "Enrolled " + learner.displayName() + ". Seats remaining: " + seats + ".",
                    null, seats, first(promotion.promotedIds), promotion.skippedIds));
        }

        waitlist.saveAndFlush(new WaitlistEntry(offeringId, learnerId, learner.displayName(), clock.instant()));
        sync(offeringId);
        int place = position(offeringId, learnerId);
        log.info("Waitlisted {} in {} at position {}", learnerId, offeringId, place);
        return remember(offeringId, learnerId, "ENROLL", key, new CommandResult(
                Outcome.WAITLISTED, offeringId, learnerId,
                "The course is full. " + learner.displayName() + " joined the waitlist at position " + place + ".",
                place, seatsRemaining(locked), first(promotion.promotedIds), promotion.skippedIds));
    }

    @Transactional
    public CommandResult drop(String offeringId, String learnerId, String idempotencyKey) {
        validateIdentity(offeringId, learnerId);
        String key = normalizeKey(idempotencyKey);
        Optional<CommandResult> replay = replay(offeringId, learnerId, "DROP", key);
        if (replay.isPresent()) {
            return replay.get();
        }

        OfferingSnapshot offering = catalog.getOffering(offeringId);
        LearnerSnapshot learner = catalog.getLearner(learnerId);
        OfferingCapacity locked = lockOrCreate(offering);
        Map<String, LearnerSnapshot> cache = new HashMap<>();
        cache.put(learner.learnerId(), learner);

        Optional<RosterEntry> seat = roster.findByOfferingIdAndLearnerId(offeringId, learnerId);
        if (seat.isPresent()) {
            roster.delete(seat.get());
            roster.flush();
            Promotion promotion = promoteUntilFull(locked, cache);
            sync(offeringId);
            String promotedId = first(promotion.promotedIds);
            log.info("Dropped {} from {}. Promoted {} skipped {}", learnerId, offeringId, promotion.promotedIds, promotion.skippedIds);
            return remember(offeringId, learnerId, "DROP", key, new CommandResult(
                    Outcome.DROPPED, offeringId, learnerId,
                    dropMessage(learner.displayName(), promotion),
                    null, seatsRemaining(locked), promotedId, promotion.skippedIds));
        }

        Optional<WaitlistEntry> waiting = waitlist.findByOfferingIdAndLearnerId(offeringId, learnerId);
        if (waiting.isPresent()) {
            waitlist.delete(waiting.get());
            waitlist.flush();
            sync(offeringId);
            return remember(offeringId, learnerId, "DROP", key, new CommandResult(
                    Outcome.LEFT_WAITLIST, offeringId, learnerId,
                    learner.displayName() + " left the waitlist. No seat was held, so nobody was promoted.",
                    null, seatsRemaining(locked), null, List.of()));
        }

        return remember(offeringId, learnerId, "DROP", key, new CommandResult(
                Outcome.NOT_ENROLLED, offeringId, learnerId,
                learner.displayName() + " is not enrolled and is not on the waitlist. Roster, capacity, and waitlist were not changed.",
                null, seatsRemaining(locked), null, List.of()));
    }

    @Transactional(readOnly = true)
    public StatusView status(String offeringId, String viewerId) {
        validateIdentity(offeringId, viewerId);
        OfferingSnapshot offering = catalog.getOffering(offeringId);
        return board(offeringId, offering, catalog.getLearner(viewerId));
    }

    @Transactional(readOnly = true)
    public StatusView overview(String offeringId, String adminId, String adminName) {
        validateIdentity(offeringId, adminId);
        OfferingSnapshot offering = catalog.getOffering(offeringId);
        String name = adminName == null || adminName.isBlank() ? adminId : adminName;
        return board(offeringId, offering, new LearnerSnapshot(adminId, name, Set.of()));
    }

    private StatusView board(String offeringId, OfferingSnapshot offering, LearnerSnapshot viewer) {
        String viewerId = viewer.learnerId();
        OfferingCapacity row = capacities.findById(offeringId).orElse(null);
        int capacity = row == null ? offering.capacity() : row.getCapacity();
        String prerequisite = row == null ? offering.prerequisiteCourseCode() : row.getPrerequisiteCourseCode();
        List<RosterEntry> seats = roster.findByOfferingIdOrderByEnrolledAtAscLearnerIdAsc(offeringId);
        List<WaitlistEntry> waiting = waitlist.findByOfferingIdOrderByEnqueuedAtAscLearnerIdAsc(offeringId);
        Map<String, LearnerSnapshot> cache = new HashMap<>();
        cache.put(viewer.learnerId(), viewer);

        List<StatusView.WaitlistSeat> waitlistSeats = new ArrayList<>();
        Integer viewerPosition = null;
        for (int index = 0; index < waiting.size(); index++) {
            WaitlistEntry entry = waiting.get(index);
            boolean eligible = isEligible(prerequisite, entry.getLearnerId(), cache);
            int position = index + 1;
            if (entry.getLearnerId().equals(viewerId)) {
                viewerPosition = position;
            }
            waitlistSeats.add(new StatusView.WaitlistSeat(
                    entry.getLearnerId(), entry.getDisplayName(), position, entry.getEnqueuedAt(), eligible));
        }

        String state = "NONE";
        if (seats.stream().anyMatch(seat -> seat.getLearnerId().equals(viewerId))) {
            state = "ENROLLED";
        } else if (viewerPosition != null) {
            state = "WAITLISTED";
        }

        int enrolledCount = seats.size();
        return new StatusView(
                offeringId,
                row == null ? offering.courseCode() : row.getCourseCode(),
                row == null ? offering.title() : row.getTitle(),
                row == null ? offering.term() : row.getTerm(),
                capacity,
                enrolledCount,
                Math.max(0, capacity - enrolledCount),
                prerequisite,
                EligibilityPolicy.DESCRIPTION,
                seats.stream().map(seat -> new StatusView.RosterSeat(seat.getLearnerId(), seat.getDisplayName(), seat.getEnrolledAt())).toList(),
                waitlistSeats,
                new StatusView.Viewer(viewer.learnerId(), viewer.displayName(),
                        rule.eligible(prerequisite, viewer.completedCourseCodes()), state, viewerPosition));
    }

    private Promotion promoteUntilFull(OfferingCapacity locked, Map<String, LearnerSnapshot> cache) {
        List<String> skipped = new ArrayList<>();
        List<WaitlistEntry> queueSnapshot = waitlist.findByOfferingIdOrderByEnqueuedAtAscLearnerIdAsc(locked.getOfferingId());
        for (WaitlistEntry entry : queueSnapshot) {
            if (!isEligible(locked, entry.getLearnerId(), cache)) {
                waitlist.delete(entry);
                skipped.add(entry.getLearnerId());
                log.info("Skipped ineligible waitlisted learner {} for {}", entry.getLearnerId(), locked.getOfferingId());
            }
        }
        if (!skipped.isEmpty()) {
            waitlist.flush();
        }

        List<String> promoted = new ArrayList<>();
        while (roster.countByOfferingId(locked.getOfferingId()) < locked.getCapacity()) {
            List<WaitlistEntry> remaining = waitlist.findByOfferingIdOrderByEnqueuedAtAscLearnerIdAsc(locked.getOfferingId());
            if (remaining.isEmpty()) {
                break;
            }
            WaitlistEntry head = remaining.get(0);
            LearnerSnapshot learner = cache.get(head.getLearnerId());
            String name = learner == null ? head.getDisplayName() : learner.displayName();
            roster.saveAndFlush(new RosterEntry(locked.getOfferingId(), head.getLearnerId(), name, clock.instant()));
            waitlist.delete(head);
            waitlist.flush();
            promoted.add(head.getLearnerId());
            log.info("Promoted {} into {}", head.getLearnerId(), locked.getOfferingId());
        }
        return new Promotion(List.copyOf(promoted), List.copyOf(skipped));
    }

    private boolean isEligible(OfferingCapacity offering, String learnerId, Map<String, LearnerSnapshot> cache) {
        return isEligible(offering.getPrerequisiteCourseCode(), learnerId, cache);
    }

    private boolean isEligible(String prerequisite, String learnerId, Map<String, LearnerSnapshot> cache) {
        try {
            LearnerSnapshot learner = cache.computeIfAbsent(learnerId, catalog::getLearner);
            return rule.eligible(prerequisite, learner.completedCourseCodes());
        } catch (LearnerNotFoundException ex) {
            return false;
        }
    }

    private OfferingCapacity lockOrCreate(OfferingSnapshot snapshot) {
        OfferingCapacity locked = capacities.lockByOfferingId(snapshot.offeringId()).orElse(null);
        if (locked == null) {
            capacities.saveAndFlush(OfferingCapacity.from(snapshot));
            locked = capacities.lockByOfferingId(snapshot.offeringId())
                    .orElseThrow(() -> new IllegalStateException("Capacity row was not created for " + snapshot.offeringId()));
        }
        locked.refreshFrom(snapshot);
        return locked;
    }

    private void sync(String offeringId) {
        List<QueuedLearner> ordered = waitlist.findByOfferingIdOrderByEnqueuedAtAscLearnerIdAsc(offeringId).stream()
                .map(entry -> new QueuedLearner(entry.getLearnerId(), entry.getEnqueuedAt()))
                .toList();
        try {
            queue.sync(offeringId, ordered);
        } catch (RuntimeException ex) {
            log.warn("FIFO cache sync failed for {}: {}", offeringId, ex.toString());
        }
    }

    private Integer position(String offeringId, String learnerId) {
        try {
            Integer cached = queue.position(offeringId, learnerId);
            if (cached != null) {
                return cached;
            }
        } catch (RuntimeException ex) {
            log.warn("FIFO cache read failed for {}: {}", offeringId, ex.toString());
        }
        List<WaitlistEntry> rows = waitlist.findByOfferingIdOrderByEnqueuedAtAscLearnerIdAsc(offeringId);
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).getLearnerId().equals(learnerId)) {
                return index + 1;
            }
        }
        return null;
    }

    private int seatsRemaining(OfferingCapacity locked) {
        return Math.max(0, locked.getCapacity() - roster.countByOfferingId(locked.getOfferingId()));
    }

    private Optional<CommandResult> replay(String offeringId, String learnerId, String operation, String key) {
        if (key == null) {
            return Optional.empty();
        }
        return idempotency.findById(idempotencyId(offeringId, learnerId, operation, key)).map(record -> {
            try {
                return objectMapper.readValue(record.getResponseJson(), CommandResult.class);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Stored idempotency response is unreadable.", ex);
            }
        });
    }

    private CommandResult remember(String offeringId, String learnerId, String operation, String key, CommandResult result) {
        if (key == null) {
            return result;
        }
        String id = idempotencyId(offeringId, learnerId, operation, key);
        if (idempotency.existsById(id)) {
            return result;
        }
        try {
            idempotency.save(new IdempotencyRecord(id, objectMapper.writeValueAsString(result), clock.instant()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not store the idempotency response.", ex);
        }
        return result;
    }

    private static String idempotencyId(String offeringId, String learnerId, String operation, String key) {
        return offeringId + "|" + learnerId + "|" + operation + "|" + key;
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String trimmed = key.trim();
        if (!IDEMPOTENCY_KEY.matcher(trimmed).matches()) {
            throw new InvalidRequestException(
                    "Idempotency-Key must be 1-80 characters and use letters, numbers, dot, underscore, colon, or hyphen.");
        }
        return trimmed;
    }

    private static void validateIdentity(String offeringId, String learnerId) {
        if (offeringId == null || offeringId.isBlank() || offeringId.length() > 40) {
            throw new InvalidRequestException("Offering id is required and must be at most 40 characters.");
        }
        if (learnerId == null || learnerId.isBlank() || learnerId.length() > 40) {
            throw new InvalidRequestException("Learner id is required and must be at most 40 characters.");
        }
    }

    private static String dropMessage(String displayName, Promotion promotion) {
        if (promotion.promotedIds.isEmpty() && promotion.skippedIds.isEmpty()) {
            return "Dropped " + displayName + ". The seat is open and nobody was waiting.";
        }
        if (promotion.promotedIds.isEmpty()) {
            return "Dropped " + displayName + ". Skipped ineligible waitlisted learners ("
                    + String.join(", ", promotion.skippedIds) + "). No eligible learner was waiting.";
        }
        String promoted = String.join(", ", promotion.promotedIds);
        if (promotion.skippedIds.isEmpty()) {
            return "Dropped " + displayName + ". Promoted " + promoted + ", the earliest eligible learner.";
        }
        return "Dropped " + displayName + ". Skipped ineligible learners ("
                + String.join(", ", promotion.skippedIds) + ") and promoted " + promoted + ".";
    }

    private static String first(List<String> ids) {
        return ids.isEmpty() ? null : ids.get(0);
    }

    private record Promotion(List<String> promotedIds, List<String> skippedIds) {
    }
}
