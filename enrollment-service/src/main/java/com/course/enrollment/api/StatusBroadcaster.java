package com.course.enrollment.api;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class StatusBroadcaster {

    private static final long TIMEOUT_MS = Duration.ofMinutes(5).toMillis();

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String offeringId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(offeringId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable remove = () -> remove(offeringId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove(offeringId, emitter));
        return emitter;
    }

    public void publish(String offeringId, StatusView view) {
        List<SseEmitter> current = emitters.get(offeringId);
        if (current == null || current.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(current)) {
            try {
                emitter.send(SseEmitter.event().name("status").data(view));
            } catch (Exception ex) {
                remove(offeringId, emitter);
            }
        }
    }

    private void remove(String offeringId, SseEmitter emitter) {
        List<SseEmitter> current = emitters.get(offeringId);
        if (current != null) {
            current.remove(emitter);
        }
    }
}
