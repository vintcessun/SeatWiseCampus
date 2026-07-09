package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seatwise.entity.Notification;
import com.seatwise.mapper.NotificationMapper;
import com.seatwise.sse.UserSseManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内通知：入库留存 + 每用户 SSE 实时推送。内容必须写明原因。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper mapper;
    private final UserSseManager userSse;

    public void notify(Long userId, String type, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setReadFlag(0);
        mapper.insert(n);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", n.getId());
        payload.put("type", type);
        payload.put("title", title);
        payload.put("content", content);
        payload.put("unread", unreadCount(userId));
        userSse.push(userId, "notification", payload);
    }

    public List<Notification> listMine(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedTime).last("limit 50"));
    }

    public long unreadCount(Long userId) {
        Long c = mapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId).eq(Notification::getReadFlag, 0));
        return c == null ? 0 : c;
    }

    public void markRead(Long userId, Long id) {
        mapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId).eq(Notification::getId, id)
                .set(Notification::getReadFlag, 1));
    }

    public void markAllRead(Long userId) {
        mapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId).eq(Notification::getReadFlag, 0)
                .set(Notification::getReadFlag, 1));
    }
}
