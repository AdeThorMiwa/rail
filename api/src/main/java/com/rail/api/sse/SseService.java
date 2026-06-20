package com.rail.api.sse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
public class SseService {

    @Value("${rail.sse.emitter-timeout-ms}")
    private long emitterTimeoutMs;

    @Value("${rail.sse.max-emitters-per-user}")
    private int maxEmittersPerUser;

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters =
        new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userPid) {
        SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
        CopyOnWriteArrayList<SseEmitter> list =
            emitters.computeIfAbsent(userPid, k -> new CopyOnWriteArrayList<>());

        if (list.size() >= maxEmittersPerUser) {
            SseEmitter oldest = list.get(0);
            list.remove(oldest);
            oldest.complete();
            log.warn("[SSE] evicted oldest emitter for user={} (cap={})", userPid, maxEmittersPerUser);
        }

        list.add(emitter);
        int count = list.size();
        log.info("[SSE] subscribe user={} totalEmitters={}", userPid, count);
        emitter.onCompletion(() -> {
            log.info("[SSE] emitter completed user={}", userPid);
            unregister(userPid, emitter);
        });
        emitter.onTimeout(() -> {
            log.warn("[SSE] emitter timeout user={}", userPid);
            unregister(userPid, emitter);
        });
        emitter.onError(e -> {
            log.error(
                "[SSE] emitter error user={} err={}",
                userPid,
                e.getMessage()
            );
            unregister(userPid, emitter);
        });
        return emitter;
    }

    public void push(UUID userPid, String eventType, Object data) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userPid);
        int emitterCount = userEmitters == null ? 0 : userEmitters.size();
        log.info(
            "[SSE] push event={} user={} emitters={}",
            eventType,
            userPid,
            emitterCount
        );
        if (userEmitters == null || userEmitters.isEmpty()) return;
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(
                    SseEmitter.event()
                        .name(eventType)
                        .data(data, MediaType.APPLICATION_JSON)
                );
                log.info("[SSE] sent event={} user={}", eventType, userPid);
            } catch (Exception e) {
                log.error(
                    "[SSE] send failed event={} user={} err={}",
                    eventType,
                    userPid,
                    e.getMessage()
                );
                dead.add(emitter);
            }
        }
        userEmitters.removeAll(dead);
    }

    @Scheduled(fixedDelay = 25_000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        for (UUID userPid : List.copyOf(emitters.keySet())) {
            CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userPid);
            if (userEmitters == null || userEmitters.isEmpty()) continue;
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    dead.add(emitter);
                }
            }
            userEmitters.removeAll(dead);
        }
    }

    private void unregister(UUID userPid, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userPid);
        if (list != null) list.remove(emitter);
        log.info(
            "[SSE] unregistered user={} remaining={}",
            userPid,
            list == null ? 0 : list.size()
        );
    }
}
