package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.common.BizError;
import com.seatwise.common.BizException;
import com.seatwise.common.SlotUtil;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.entity.Reservation;
import com.seatwise.entity.Seat;
import com.seatwise.entity.StudyRoom;
import com.seatwise.entity.Waitlist;
import com.seatwise.mapper.ReservationMapper;
import com.seatwise.mapper.SeatMapper;
import com.seatwise.mapper.StudyRoomMapper;
import com.seatwise.mapper.WaitlistMapper;
import com.seatwise.sse.SseManager;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 候补队列：无空位时排队；座位释放（取消/超时/签退/自动完成）自动匹配队首并临时保留 60 秒，
 * SSE + 通知提醒，超时顺延下一位。把超时释放/取消/推送/临时锁串成完整闭环。
 */
@Service
@RequiredArgsConstructor
public class WaitlistService {

    private static final int OFFER_SECONDS = 60;

    private final WaitlistMapper mapper;
    private final RedissonClient redisson;
    private final SeatMapper seatMapper;
    private final StudyRoomMapper roomMapper;
    private final ReservationMapper reservationMapper;
    private final NotificationService notificationService;
    private final SseManager sse;
    private final SeatwiseProps props;

    @Autowired @Lazy
    private ReservationService reservationService;

    public Waitlist join(Long userId, Long roomId, LocalDate date, LocalTime start, LocalTime end) {
        int slotMin = props.getSlotMinutes();
        int startSlot = SlotUtil.toSlot(start, slotMin);
        int endSlot = SlotUtil.toSlot(end, slotMin);
        if (startSlot >= endSlot) throw new BizException(BizError.INVALID_TIME_RANGE);
        Waitlist exist = mapper.selectOne(new LambdaQueryWrapper<Waitlist>()
                .eq(Waitlist::getUserId, userId).eq(Waitlist::getRoomId, roomId).eq(Waitlist::getDate, date)
                .eq(Waitlist::getStartSlot, startSlot).eq(Waitlist::getEndSlot, endSlot)
                .in(Waitlist::getStatus, List.of("WAITING", "OFFERED")).last("limit 1"));
        if (exist != null) return exist;
        Waitlist w = new Waitlist();
        w.setUserId(userId); w.setRoomId(roomId); w.setDate(date);
        w.setStartSlot(startSlot); w.setEndSlot(endSlot); w.setStatus("WAITING");
        mapper.insert(w);
        return w;
    }

    public List<Waitlist> listMine(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<Waitlist>()
                .eq(Waitlist::getUserId, userId)
                .orderByDesc(Waitlist::getCreatedTime).last("limit 30"));
    }

    public void cancel(Long userId, Long id) {
        Waitlist w = mapper.selectById(id);
        if (w == null || !w.getUserId().equals(userId)) throw new BizException(BizError.PERMISSION_DENIED);
        if ("OFFERED".equals(w.getStatus()) && w.getOfferedSeatId() != null) {
            releaseHold(w.getRoomId(), w.getDate(), w.getOfferedSeatId());
        }
        w.setStatus("CANCELLED");
        mapper.updateById(w);
    }

    public void accept(Long userId, Long id) {
        Waitlist w = mapper.selectById(id);
        if (w == null || !w.getUserId().equals(userId)) throw new BizException(BizError.PERMISSION_DENIED);
        if (!"OFFERED".equals(w.getStatus()) || w.getOfferedSeatId() == null
                || w.getOfferExpireAt() == null || w.getOfferExpireAt().isBefore(LocalDateTime.now()))
            throw new BizException(BizError.WAITLIST_INVALID, "该候补席位已失效");
        int slotMin = props.getSlotMinutes();
        LocalTime start = SlotUtil.slotToTime(w.getStartSlot(), slotMin);
        LocalTime end = SlotUtil.slotToTime(w.getEndSlot(), slotMin);
        // 复用预约流程（唯一索引兜底）
        reservationService.create(userId, w.getRoomId(), w.getOfferedSeatId(), w.getDate(), start, end);
        w.setStatus("FULFILLED");
        mapper.updateById(w);
    }

    /** 座位释放时匹配队首候补并保留 */
    public void onSeatReleased(Long roomId, LocalDate date, Long seatId, int relStart, int relEnd) {
        Waitlist w = mapper.selectOne(new LambdaQueryWrapper<Waitlist>()
                .eq(Waitlist::getRoomId, roomId).eq(Waitlist::getDate, date).eq(Waitlist::getStatus, "WAITING")
                .ge(Waitlist::getStartSlot, relStart).le(Waitlist::getEndSlot, relEnd)
                .orderByAsc(Waitlist::getCreatedTime).last("limit 1"));
        if (w == null) return;
        // 座位需在候补时段内确实空闲
        List<Reservation> active = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, seatId).eq(Reservation::getDate, date)
                .in(Reservation::getStatus, List.of("PENDING_SIGN_IN", "IN_USE")));
        for (Reservation r : active)
            if (r.getStartSlot() < w.getEndSlot() && r.getEndSlot() > w.getStartSlot()) return;

        // 临时保留该座位给候补者
        redisson.getBucket(BoardService.holdKey(roomId, date, seatId)).set(w.getUserId(), Duration.ofSeconds(OFFER_SECONDS));
        long expireAt = System.currentTimeMillis() + OFFER_SECONDS * 1000L;
        w.setStatus("OFFERED"); w.setOfferedSeatId(seatId); w.setOfferExpireAt(LocalDateTime.now().plusSeconds(OFFER_SECONDS));
        mapper.updateById(w);

        Seat seat = seatMapper.selectById(seatId);
        StudyRoom room = roomMapper.selectById(roomId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId); payload.put("date", date.toString());
        payload.put("seatId", seatId); payload.put("seatNo", seat != null ? seat.getSeatNo() : null);
        payload.put("byUserId", w.getUserId()); payload.put("expireAt", expireAt);
        sse.broadcast(roomId, date, "seat_hold", payload);

        notificationService.notify(w.getUserId(), "WAITLIST", "候补席位已为你保留",
                "「" + (room != null ? room.getName() : "") + "」" + (seat != null ? seat.getSeatNo() : "")
                        + " 已空出，请 " + OFFER_SECONDS + " 秒内在「我的候补」确认");
    }

    private void releaseHold(Long roomId, LocalDate date, Long seatId) {
        redisson.getBucket(BoardService.holdKey(roomId, date, seatId)).delete();
        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId); payload.put("date", date.toString()); payload.put("seatId", seatId);
        sse.broadcast(roomId, date, "hold_released", payload);
    }

    /** 定时清理超时未确认的候补保留，并顺延下一位 */
    public void expireOffers() {
        List<Waitlist> expired = mapper.selectList(new LambdaQueryWrapper<Waitlist>()
                .eq(Waitlist::getStatus, "OFFERED").lt(Waitlist::getOfferExpireAt, LocalDateTime.now()));
        for (Waitlist w : expired) {
            Long seatId = w.getOfferedSeatId();
            releaseHold(w.getRoomId(), w.getDate(), seatId);
            w.setStatus("EXPIRED");
            mapper.updateById(w);
            notificationService.notify(w.getUserId(), "WAITLIST", "候补保留已超时", "未在时限内确认，席位已释放给下一位");
            if (seatId != null) onSeatReleased(w.getRoomId(), w.getDate(), seatId, w.getStartSlot(), w.getEndSlot());
        }
    }
}
