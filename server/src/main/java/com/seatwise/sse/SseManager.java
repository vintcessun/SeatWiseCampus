package com.seatwise.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * SSE 连接管理：按 roomId + date 维护订阅者集合，广播座位状态增量事件。
 */
@Slf4j
@Component
public class SseManager {

    private final Map<String, Set<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String key(Long roomId, LocalDate date) {
        return roomId + "@" + date;
    }

    public SseEmitter subscribe(Long roomId, LocalDate date) {
        SseEmitter emitter = new SseEmitter(0L); // 不超时，靠心跳与客户端控制
        String k = key(roomId, date);
        subscribers.computeIfAbsent(k, x -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> remove(k, emitter));
        emitter.onTimeout(() -> remove(k, emitter));
        emitter.onError(e -> remove(k, emitter));
        return emitter;
    }

    private void remove(String k, SseEmitter emitter) {
        Set<SseEmitter> set = subscribers.get(k);
        if (set != null) {
            set.remove(emitter);
        }
    }

    /** 广播座位状态事件到指定房间+日期的订阅者 */
    public void broadcast(Long roomId, LocalDate date, String event, Object payload) {
        Set<SseEmitter> set = subscribers.get(key(roomId, date));
        if (set == null || set.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("SSE 序列化失败", e);
            return;
        }
        for (SseEmitter emitter : set) {
            try {
                emitter.send(SseEmitter.event().name(event).data(json));
            } catch (IOException | IllegalStateException e) {
                remove(key(roomId, date), emitter);
            }
        }
    }

    /** 心跳，保活所有连接 */
    public void heartbeatAll() {
        subscribers.forEach((k, set) -> {
            for (SseEmitter emitter : set) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("{\"ts\":" + System.currentTimeMillis() + "}"));
                } catch (IOException | IllegalStateException e) {
                    remove(k, emitter);
                }
            }
        });
    }
}
