package com.seatwise.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 每用户 SSE 通道：用于站内通知的实时推送（与看板 SSE 分离）。
 */
@Slf4j
@Component
public class UserSseManager {

    private final Map<Long, Set<SseEmitter>> subs = new ConcurrentHashMap<>();
    private final ObjectMapper om = new ObjectMapper();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        subs.computeIfAbsent(userId, x -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        return emitter;
    }

    private void remove(Long userId, SseEmitter e) {
        Set<SseEmitter> s = subs.get(userId);
        if (s != null) s.remove(e);
    }

    public void push(Long userId, String event, Object payload) {
        Set<SseEmitter> s = subs.get(userId);
        if (s == null || s.isEmpty()) return;
        String json;
        try { json = om.writeValueAsString(payload); } catch (Exception ex) { return; }
        for (SseEmitter e : s) {
            try { e.send(SseEmitter.event().name(event).data(json)); }
            catch (IOException | IllegalStateException ex) { remove(userId, e); }
        }
    }

    public void heartbeatAll() {
        subs.forEach((uid, set) -> {
            for (SseEmitter e : set) {
                try { e.send(SseEmitter.event().name("heartbeat").data("{}")); }
                catch (IOException | IllegalStateException ex) { remove(uid, e); }
            }
        });
    }
}
