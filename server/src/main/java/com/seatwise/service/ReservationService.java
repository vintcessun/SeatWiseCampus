package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.common.BizError;
import com.seatwise.common.BizException;
import com.seatwise.common.SlotUtil;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.entity.*;
import com.seatwise.mapper.*;
import com.seatwise.sse.SseManager;
import com.seatwise.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final ReservationSlotMapper slotMapper;
    private final SeatMapper seatMapper;
    private final StudyRoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final BlacklistMapper blacklistMapper;
    private final UserMapper userMapper;
    private final RedissonClient redisson;
    private final SseManager sse;
    private final ScoreService scoreService;
    private final NotificationService notificationService;
    private final WaitlistService waitlistService;
    private final SeatwiseProps props;
    private final TransactionTemplate tx;

    // ===================== 预约 =====================
    public ReservationVO create(Long userId, Long roomId, Long seatId, LocalDate date,
                                LocalTime startTime, LocalTime endTime) {
        int slotMin = props.getSlotMinutes();

        // 3. 黑名单
        checkBlacklist(userId);

        // 4. 时间校验
        StudyRoom room = roomMapper.selectById(roomId);
        if (room == null) throw new BizException(BizError.INVALID_TIME_RANGE, "自习室不存在");
        if (!startTime.isBefore(endTime)) throw new BizException(BizError.INVALID_TIME_RANGE);
        if (startTime.getMinute() % slotMin != 0 || endTime.getMinute() % slotMin != 0)
            throw new BizException(BizError.INVALID_TIME_RANGE, "时间需按 " + slotMin + " 分钟对齐");
        int startSlot = SlotUtil.toSlot(startTime, slotMin);
        int endSlot = SlotUtil.toSlot(endTime, slotMin);
        if (startSlot >= endSlot) throw new BizException(BizError.INVALID_TIME_RANGE);
        if (room.getOpenStart() != null && startTime.isBefore(room.getOpenStart()))
            throw new BizException(BizError.INVALID_TIME_RANGE, "早于开放时间");
        if (room.getOpenEnd() != null && endTime.isAfter(room.getOpenEnd()))
            throw new BizException(BizError.INVALID_TIME_RANGE, "晚于开放时间");
        LocalDateTime nowTs = LocalDateTime.now();
        if (LocalDateTime.of(date, startTime).isBefore(nowTs))
            throw new BizException(BizError.INVALID_TIME_RANGE, "预约开始时间需晚于当前时间");

        // 5. 单次时长 & 单日次数
        if (endSlot - startSlot > props.getMaxSlotsPerReservation())
            throw new BizException(BizError.INVALID_TIME_RANGE, "单次预约时长超上限");
        Long dayCount = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId).eq(Reservation::getDate, date)
                .in(Reservation::getStatus, List.of("PENDING_SIGN_IN", "IN_USE", "COMPLETED")));
        if (dayCount != null && dayCount >= props.getDailyLimit())
            throw new BizException(BizError.DAILY_LIMIT_EXCEEDED);

        // 6. 自身时段冲突
        List<Reservation> mine = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId).eq(Reservation::getDate, date)
                .in(Reservation::getStatus, List.of("PENDING_SIGN_IN", "IN_USE")));
        for (Reservation r : mine) {
            if (r.getStartSlot() < endSlot && r.getEndSlot() > startSlot)
                throw new BizException(BizError.RESERVATION_TIME_CONFLICT);
        }

        // 座位可预约校验
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null || !"SEAT".equals(seat.getCellType()) || seat.getEnabled() == null || seat.getEnabled() == 0)
            throw new BizException(BizError.BAD_REQUEST, "座位不可预约");

        // 7. slot 列表
        List<Integer> slots = SlotUtil.expand(startSlot, endSlot);

        // 8-9. Redisson 锁
        String lockKey = "seat:" + seatId + ":date:" + date + ":slots:" + startSlot + "-" + endSlot;
        RLock lock = redisson.getLock(lockKey);
        boolean locked = false;
        try {
            try {
                locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 10-13. 事务内写入（唯一索引兜底）
            Reservation reservation;
            try {
                reservation = tx.execute(status -> {
                    Reservation r = new Reservation();
                    r.setUserId(userId);
                    r.setSeatId(seatId);
                    r.setRoomId(roomId);
                    r.setDate(date);
                    r.setStartSlot(startSlot);
                    r.setEndSlot(endSlot);
                    r.setStatus("PENDING_SIGN_IN");
                    reservationMapper.insert(r);
                    for (Integer s : slots) {
                        ReservationSlot rs = new ReservationSlot();
                        rs.setReservationId(r.getId());
                        rs.setSeatId(seatId);
                        rs.setDate(date);
                        rs.setSlotIndex(s);
                        slotMapper.insert(rs);   // 唯一键冲突 -> DuplicateKeyException
                    }
                    return r;
                });
            } catch (DuplicateKeyException e) {
                throw new BizException(BizError.SEAT_ALREADY_RESERVED);
            }
            // 清理临时锁座 + SSE 推送
            redisson.getBucket(BoardService.holdKey(roomId, date, seatId)).delete();
            broadcastSeat(roomId, date, seatId, "RESERVED", "seat_reserved");
            return toVO(reservation);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    // ===================== 签到 =====================
    public ReservationVO checkIn(Long userId, Long reservationId) {
        Reservation r = mustOwn(userId, reservationId);
        if (!"PENDING_SIGN_IN".equals(r.getStatus()))
            throw new BizException(BizError.RESERVATION_NOT_FOUND, "当前状态不可签到");
        LocalDateTime start = startDateTime(r);
        LocalDateTime deadline = start.plusMinutes(props.getSigninWindowMinutes());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(start))
            throw new BizException(BizError.SIGN_IN_TOO_EARLY,
                    "签到将于 " + SlotUtil.label(r.getStartSlot(), props.getSlotMinutes()) + " 开放");
        if (now.isAfter(deadline))
            throw new BizException(BizError.SIGN_IN_TIMEOUT);
        tx.executeWithoutResult(s -> {
            r.setStatus("IN_USE");
            r.setCheckInTime(LocalDateTime.now());
            reservationMapper.updateById(r);
        });
        broadcastSeat(r.getRoomId(), r.getDate(), r.getSeatId(), "USING", "seat_in_use");
        return toVO(r);
    }

    // ===================== 签退 =====================
    public ReservationVO checkOut(Long userId, Long reservationId) {
        Reservation r = mustOwn(userId, reservationId);
        if (!"IN_USE".equals(r.getStatus()))
            throw new BizException(BizError.RESERVATION_NOT_FOUND, "当前状态不可签退");
        tx.executeWithoutResult(s -> {
            r.setStatus("COMPLETED");
            r.setCheckOutTime(LocalDateTime.now());
            reservationMapper.updateById(r);
            releaseSlots(r.getId());
        });
        scoreService.addScore(userId, 2, "CHECKOUT_OK", r.getId());
        notificationService.notify(userId, "SCORE", "积分 +2",
                "按时签退并完成本次预约（" + seatLabel(r) + "）");
        broadcastSeat(r.getRoomId(), r.getDate(), r.getSeatId(), "FREE", "seat_released");
        waitlistService.onSeatReleased(r.getRoomId(), r.getDate(), r.getSeatId(), r.getStartSlot(), r.getEndSlot());
        ReservationVO vo = toVO(r);
        vo.setScoreDelta(2);
        return vo;
    }

    // ===================== 取消 =====================
    public ReservationVO cancel(Long userId, Long reservationId) {
        Reservation r = mustOwn(userId, reservationId);
        if (!List.of("PENDING_SIGN_IN", "IN_USE").contains(r.getStatus()))
            throw new BizException(BizError.RESERVATION_NOT_FOUND, "当前状态不可取消");
        boolean late = LocalDateTime.now().isAfter(startDateTime(r).minusMinutes(30));
        tx.executeWithoutResult(s -> {
            r.setStatus("CANCELLED");
            reservationMapper.updateById(r);
            releaseSlots(r.getId());
        });
        if (late) {
            scoreService.addScore(userId, -1, "CANCEL_LATE", r.getId());
            notificationService.notify(userId, "SCORE", "积分 -1",
                    "预约开始前 30 分钟内取消（" + seatLabel(r) + "）");
        }
        broadcastSeat(r.getRoomId(), r.getDate(), r.getSeatId(), "FREE", "seat_released");
        waitlistService.onSeatReleased(r.getRoomId(), r.getDate(), r.getSeatId(), r.getStartSlot(), r.getEndSlot());
        ReservationVO vo = toVO(r);
        vo.setScoreDelta(late ? -1 : 0);
        return vo;
    }

    // ===================== 超时释放（定时任务调用） =====================
    public void releaseTimeout(Reservation r) {
        Long userId = r.getUserId();
        tx.executeWithoutResult(s -> {
            r.setStatus("EXPIRED_RELEASED");
            reservationMapper.updateById(r);
            releaseSlots(r.getId());
            User u = userMapper.selectById(userId);
            if (u != null) {
                u.setNoShowCount((u.getNoShowCount() == null ? 0 : u.getNoShowCount()) + 1);
                userMapper.updateById(u);
                if (u.getNoShowCount() >= props.getNoshowThreshold()) {
                    ensureBlacklist(u);
                }
            }
        });
        scoreService.addScore(userId, -3, "NO_SHOW", r.getId());
        notificationService.notify(userId, "SCORE", "积分 -3",
                "预约开始后 15 分钟内未签到，座位已释放（" + seatLabel(r) + "）");
        User u2 = userMapper.selectById(userId);
        if (u2 != null && u2.getNoShowCount() != null && u2.getNoShowCount() >= props.getNoshowThreshold()) {
            notificationService.notify(userId, "BLACKLIST", "已进入黑名单",
                    "爽约累计达 " + props.getNoshowThreshold() + " 次，" + props.getBlacklistDays()
                            + " 天内暂不能预约，可继续登录与查看记录");
        }
        broadcastSeat(r.getRoomId(), r.getDate(), r.getSeatId(), "FREE", "seat_released");
        waitlistService.onSeatReleased(r.getRoomId(), r.getDate(), r.getSeatId(), r.getStartSlot(), r.getEndSlot());
    }

    // ===================== 自动完成（定时任务调用） =====================
    public void autoComplete(Reservation r) {
        tx.executeWithoutResult(s -> {
            r.setStatus("COMPLETED");
            r.setCheckOutTime(LocalDateTime.now());
            reservationMapper.updateById(r);
            releaseSlots(r.getId());
        });
        scoreService.addScore(r.getUserId(), 2, "CHECKOUT_OK", r.getId());
        broadcastSeat(r.getRoomId(), r.getDate(), r.getSeatId(), "FREE", "seat_released");
    }

    // ===================== 我的预约 =====================
    public List<ReservationVO> myReservations(Long userId) {
        List<Reservation> list = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getDate).orderByDesc(Reservation::getStartSlot));
        List<ReservationVO> vos = new ArrayList<>();
        for (Reservation r : list) vos.add(toVO(r));
        return vos;
    }

    // ===================== helpers =====================
    private Reservation mustOwn(Long userId, Long reservationId) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) throw new BizException(BizError.RESERVATION_NOT_FOUND);
        if (!r.getUserId().equals(userId)) throw new BizException(BizError.PERMISSION_DENIED);
        return r;
    }

    private String seatLabel(Reservation r) {
        Seat seat = seatMapper.selectById(r.getSeatId());
        StudyRoom room = roomMapper.selectById(r.getRoomId());
        return (room != null ? room.getName() : "") + " " + (seat != null ? seat.getSeatNo() : "");
    }

    private void releaseSlots(Long reservationId) {
        slotMapper.delete(new LambdaQueryWrapper<ReservationSlot>()
                .eq(ReservationSlot::getReservationId, reservationId));
    }

    private void checkBlacklist(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<BlacklistRecord> list = blacklistMapper.selectList(new LambdaQueryWrapper<BlacklistRecord>()
                .eq(BlacklistRecord::getUserId, userId).eq(BlacklistRecord::getActive, 1));
        for (BlacklistRecord b : list) {
            if (b.getEndTime() != null && b.getEndTime().isAfter(now)) {
                throw new BizException(BizError.USER_IN_BLACKLIST);
            } else {
                b.setActive(0);
                blacklistMapper.updateById(b);
            }
        }
    }

    private void ensureBlacklist(User u) {
        Long active = blacklistMapper.selectCount(new LambdaQueryWrapper<BlacklistRecord>()
                .eq(BlacklistRecord::getUserId, u.getId()).eq(BlacklistRecord::getActive, 1));
        if (active != null && active > 0) return;
        BlacklistRecord b = new BlacklistRecord();
        b.setUserId(u.getId());
        b.setReason("爽约次数达到阈值 " + props.getNoshowThreshold());
        b.setStartTime(LocalDateTime.now());
        b.setEndTime(LocalDateTime.now().plusDays(props.getBlacklistDays()));
        b.setActive(1);
        blacklistMapper.insert(b);
    }

    private LocalDateTime startDateTime(Reservation r) {
        return LocalDateTime.of(r.getDate(), SlotUtil.slotToTime(r.getStartSlot(), props.getSlotMinutes()));
    }

    private LocalDateTime endDateTime(Reservation r) {
        return LocalDateTime.of(r.getDate(), SlotUtil.slotToTime(r.getEndSlot(), props.getSlotMinutes()));
    }

    private void broadcastSeat(Long roomId, LocalDate date, Long seatId, String status, String event) {
        Seat seat = seatMapper.selectById(seatId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId);
        payload.put("date", date.toString());
        payload.put("seatId", seatId);
        payload.put("seatNo", seat != null ? seat.getSeatNo() : null);
        payload.put("status", status);
        sse.broadcast(roomId, date, event, payload);
    }

    private ReservationVO toVO(Reservation r) {
        ReservationVO vo = new ReservationVO();
        vo.setId(r.getId());
        vo.setSeatId(r.getSeatId());
        vo.setRoomId(r.getRoomId());
        vo.setDate(r.getDate());
        vo.setStartTime(SlotUtil.label(r.getStartSlot(), props.getSlotMinutes()));
        vo.setEndTime(SlotUtil.label(r.getEndSlot(), props.getSlotMinutes()));
        vo.setSigninStart(vo.getStartTime());
        LocalTime dl = SlotUtil.slotToTime(r.getStartSlot(), props.getSlotMinutes())
                .plusMinutes(props.getSigninWindowMinutes());
        vo.setSigninDeadline(String.format("%02d:%02d", dl.getHour(), dl.getMinute()));
        vo.setStatus(r.getStatus());
        vo.setCheckInTime(r.getCheckInTime() != null ? r.getCheckInTime().toString() : null);
        vo.setCheckOutTime(r.getCheckOutTime() != null ? r.getCheckOutTime().toString() : null);
        Seat seat = seatMapper.selectById(r.getSeatId());
        if (seat != null) vo.setSeatNo(seat.getSeatNo());
        StudyRoom room = roomMapper.selectById(r.getRoomId());
        if (room != null) {
            vo.setRoomName(room.getName());
            Building b = buildingMapper.selectById(room.getBuildingId());
            if (b != null) vo.setBuildingName(b.getName());
        }
        return vo;
    }

    // ===================== 管理端：按学生追踪预约 =====================
    public List<ReservationVO> adminSearch(String keyword, String status, LocalDate date) {
        LambdaQueryWrapper<Reservation> w = new LambdaQueryWrapper<Reservation>()
                .orderByDesc(Reservation::getDate).orderByDesc(Reservation::getStartSlot).last("limit 200");
        if (status != null && !status.isBlank()) w.eq(Reservation::getStatus, status);
        if (date != null) w.eq(Reservation::getDate, date);
        if (keyword != null && !keyword.isBlank()) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .like(User::getRealName, keyword).or().like(User::getUsername, keyword));
            List<Long> ids = users.stream().map(User::getId).toList();
            if (ids.isEmpty()) return List.of();
            w.in(Reservation::getUserId, ids);
        }
        List<Reservation> list = reservationMapper.selectList(w);
        List<ReservationVO> vos = new ArrayList<>();
        for (Reservation r : list) {
            ReservationVO vo = toVO(r);
            User u = userMapper.selectById(r.getUserId());
            if (u != null) { vo.setStudentName(u.getRealName()); vo.setUsername(u.getUsername()); }
            vos.add(vo);
        }
        return vos;
    }

    // 供定时任务查询
    public List<Reservation> findByStatus(String status) {
        return reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, status));
    }

    public LocalDateTime signinDeadline(Reservation r) {
        return startDateTime(r).plusMinutes(props.getSigninWindowMinutes());
    }

    public LocalDateTime reservationEnd(Reservation r) {
        return endDateTime(r);
    }
}
