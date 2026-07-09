package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.common.SlotUtil;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.entity.Reservation;
import com.seatwise.entity.Seat;
import com.seatwise.entity.StudyRoom;
import com.seatwise.mapper.ReservationMapper;
import com.seatwise.mapper.SeatMapper;
import com.seatwise.mapper.StudyRoomMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 预约提醒：定时扫描待签到预约，
 * - 开始前 remindBeforeMinutes 分钟推「即将开始」；
 * - 签到窗口开放（到达开始时间）推「可以签到了」。
 * 用 Redis 键去重，保证每类提醒每预约只推一次。复用通知中心（type=REMINDER）。
 */
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final StudyRoomMapper roomMapper;
    private final NotificationService notificationService;
    private final SeatwiseProps props;
    private final RedissonClient redisson;

    public void runReminders() {
        int slotMin = props.getSlotMinutes();
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> pending = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, "PENDING_SIGN_IN"));
        for (Reservation r : pending) {
            LocalDateTime start = LocalDateTime.of(r.getDate(), SlotUtil.slotToTime(r.getStartSlot(), slotMin));
            LocalDateTime deadline = start.plusMinutes(props.getSigninWindowMinutes());
            LocalDateTime soonFrom = start.minusMinutes(props.getRemindBeforeMinutes());

            // 即将开始
            if (!now.isBefore(soonFrom) && now.isBefore(start)
                    && once("remind:soon:" + r.getId())) {
                long mins = Math.max(1, Duration.between(now, start).toMinutes());
                notificationService.notify(r.getUserId(), "REMINDER", "⏰ 预约即将开始",
                        label(r) + " 将于 " + SlotUtil.label(r.getStartSlot(), slotMin)
                                + " 开始（约 " + mins + " 分钟后），请准时前往签到。");
            }
            // 签到开放
            if (!now.isBefore(start) && !now.isAfter(deadline)
                    && once("remind:signin:" + r.getId())) {
                notificationService.notify(r.getUserId(), "REMINDER", "✅ 现在可以签到了",
                        label(r) + " 签到已开放，请在 "
                                + String.format("%02d:%02d", deadline.getHour(), deadline.getMinute())
                                + " 前完成签到，超时将自动释放并计入爽约。");
            }
        }
    }

    /** Redis 去重：首次返回 true，之后返回 false（TTL 6 小时） */
    private boolean once(String key) {
        return redisson.getBucket(key).trySet(1, 6, TimeUnit.HOURS);
    }

    private String label(Reservation r) {
        Seat seat = seatMapper.selectById(r.getSeatId());
        StudyRoom room = roomMapper.selectById(r.getRoomId());
        return "「" + (room != null ? room.getName() : "") + "」" + (seat != null ? seat.getSeatNo() : "");
    }
}
