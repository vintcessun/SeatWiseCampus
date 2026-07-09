package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.common.BizError;
import com.seatwise.common.BizException;
import com.seatwise.common.SlotUtil;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.entity.Reservation;
import com.seatwise.entity.Seat;
import com.seatwise.mapper.ReservationMapper;
import com.seatwise.mapper.SeatMapper;
import com.seatwise.sse.SseManager;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 临时锁座：点座即用 Redis TTL 抢占式占位，SSE 广播「正在被选择」，到期自动释放。
 * 展示分布式动态性；最终正确性仍由预约唯一索引兜底。
 */
@Service
@RequiredArgsConstructor
public class HoldService {

    private final RedissonClient redisson;
    private final SeatMapper seatMapper;
    private final ReservationMapper reservationMapper;
    private final SseManager sse;
    private final SeatwiseProps props;

    public long hold(Long userId, Long roomId, Long seatId, LocalDate date, LocalTime start, LocalTime end) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null || !"SEAT".equals(seat.getCellType()) || seat.getEnabled() == null || seat.getEnabled() == 0)
            throw new BizException(BizError.BAD_REQUEST, "座位不可选");

        int slotMin = props.getSlotMinutes();
        int startSlot = SlotUtil.toSlot(start, slotMin);
        int endSlot = SlotUtil.toSlot(end, slotMin);
        // 该时段是否已被正式预约
        List<Reservation> active = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, seatId).eq(Reservation::getDate, date)
                .in(Reservation::getStatus, List.of("PENDING_SIGN_IN", "IN_USE")));
        for (Reservation r : active) {
            if (r.getStartSlot() < endSlot && r.getEndSlot() > startSlot)
                throw new BizException(BizError.SEAT_ALREADY_RESERVED);
        }

        RBucket<Object> bucket = redisson.getBucket(BoardService.holdKey(roomId, date, seatId));
        Object holder = bucket.get();
        if (holder != null && !userId.toString().equals(holder.toString()))
            throw new BizException(BizError.SEAT_ALREADY_HELD);

        bucket.set(userId, Duration.ofSeconds(props.getHoldSeconds()));
        long expireAt = System.currentTimeMillis() + props.getHoldSeconds() * 1000L;

        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId);
        payload.put("date", date.toString());
        payload.put("seatId", seatId);
        payload.put("seatNo", seat.getSeatNo());
        payload.put("byUserId", userId);
        payload.put("expireAt", expireAt);
        sse.broadcast(roomId, date, "seat_hold", payload);
        return expireAt;
    }

    public void release(Long userId, Long roomId, Long seatId, LocalDate date) {
        RBucket<Object> bucket = redisson.getBucket(BoardService.holdKey(roomId, date, seatId));
        Object holder = bucket.get();
        if (holder != null && userId.toString().equals(holder.toString())) {
            bucket.delete();
            Map<String, Object> payload = new HashMap<>();
            payload.put("roomId", roomId);
            payload.put("date", date.toString());
            payload.put("seatId", seatId);
            sse.broadcast(roomId, date, "hold_released", payload);
        }
    }

    /** 预约成功后清理该座位的锁（无需广播，seat_reserved 已发） */
    public void clear(Long roomId, Long seatId, LocalDate date) {
        redisson.getBucket(BoardService.holdKey(roomId, date, seatId)).delete();
    }
}
