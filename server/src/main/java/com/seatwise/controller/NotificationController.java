package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.seatwise.common.R;
import com.seatwise.entity.Notification;
import com.seatwise.service.NotificationService;
import com.seatwise.sse.UserSseManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;
    private final UserSseManager userSse;

    private Long uid() { return Long.valueOf(StpUtil.getLoginId().toString()); }

    @SaCheckLogin
    @GetMapping
    public R<List<Notification>> list() {
        return R.ok(service.listMine(uid()));
    }

    @SaCheckLogin
    @GetMapping("/unread-count")
    public R<Map<String, Object>> unread() {
        return R.ok(Map.of("unread", service.unreadCount(uid())));
    }

    @SaCheckLogin
    @PostMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        service.markRead(uid(), id);
        return R.ok();
    }

    @SaCheckLogin
    @PostMapping("/read-all")
    public R<Void> readAll() {
        service.markAllRead(uid());
        return R.ok();
    }

    /** 每用户通知 SSE（token 走查询参数，EventSource 不能自定义头） */
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter stream(@RequestParam String token) {
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            SseEmitter e = new SseEmitter(0L);
            e.complete();
            return e;
        }
        return userSse.subscribe(Long.valueOf(loginId.toString()));
    }
}
