package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.entity.*;
import com.seatwise.mapper.*;
import com.seatwise.common.SlotUtil;
import com.seatwise.vo.BoardVO;
import com.seatwise.vo.ReplayVO;
import com.seatwise.vo.SeatStatusVO;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final SeatMapper seatMapper;
    private final StudyRoomMapper studyRoomMapper;
    private final ReservationMapper reservationMapper;
    private final SeatwiseProps props;
    private final RedissonClient redisson;

    /** 临时锁座 Redis key */
    public static String holdKey(Long roomId, LocalDate date, Long seatId) {
        return "hold:" + roomId + ":" + date + ":" + seatId;
    }

    /**
     * 构建看板快照。currentUserId 可空（未登录/管理员查看）。
     */
    public BoardVO buildBoard(Long roomId, LocalDate date, int startSlot, int endSlot, Long currentUserId) {
        StudyRoom room = studyRoomMapper.selectById(roomId);
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getRoomId, roomId)
                .orderByAsc(Seat::getRowIndex).orderByAsc(Seat::getColIndex));

        // 该房间当天处于占用中的预约（待签到/使用中）
        List<Reservation> active = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getRoomId, roomId)
                .eq(Reservation::getDate, date)
                .in(Reservation::getStatus, List.of("PENDING_SIGN_IN", "IN_USE")));

        // seatId -> 占用信息（与请求时段有交叠者）
        Map<Long, Reservation> occupied = new HashMap<>();
        for (Reservation r : active) {
            boolean overlap = r.getStartSlot() < endSlot && r.getEndSlot() > startSlot;
            if (overlap) {
                occupied.put(r.getSeatId(), r);
            }
        }

        int rows = 0, cols = 0;
        List<SeatStatusVO> list = new ArrayList<>();
        for (Seat seat : seats) {
            rows = Math.max(rows, seat.getRowIndex() + 1);
            cols = Math.max(cols, seat.getColIndex() + 1);
            SeatStatusVO vo = new SeatStatusVO();
            BeanUtils.copyProperties(seat, vo);
            vo.setSeatId(seat.getId());
            vo.setMine(false);
            if (!"SEAT".equals(seat.getCellType())) {
                vo.setStatus(seat.getCellType());
            } else if (seat.getEnabled() == null || seat.getEnabled() == 0) {
                vo.setStatus("DISABLED");
            } else {
                Reservation r = occupied.get(seat.getId());
                if (r == null) {
                    vo.setStatus("FREE");
                    // 临时锁座（Redis TTL）
                    RBucket<Object> hold = redisson.getBucket(holdKey(roomId, date, seat.getId()));
                    Object holder = hold.get();
                    if (holder != null) {
                        long ttl = hold.remainTimeToLive();
                        if (ttl > 0) {
                            vo.setStatus("HELD");
                            Long by = Long.valueOf(holder.toString());
                            vo.setHeldBy(by);
                            vo.setHoldExpireAt(System.currentTimeMillis() + ttl);
                            vo.setMine(currentUserId != null && currentUserId.equals(by));
                        }
                    }
                } else {
                    vo.setStatus("IN_USE".equals(r.getStatus()) ? "USING" : "RESERVED");
                    vo.setMine(currentUserId != null && currentUserId.equals(r.getUserId()));
                }
            }
            list.add(vo);
        }

        BoardVO board = new BoardVO();
        board.setRoomId(roomId);
        board.setRoomName(room != null ? room.getName() : "");
        board.setDate(date);
        board.setRows(rows);
        board.setCols(cols);
        board.setStartSlot(startSlot);
        board.setEndSlot(endSlot);
        board.setSeats(list);
        return board;
    }

    /** 统计房间在某时段的空闲 SEAT 数量（用于附近空位推荐/概览） */
    public int countAvailable(Long roomId, LocalDate date, int startSlot, int endSlot) {
        BoardVO board = buildBoard(roomId, date, startSlot, endSlot, null);
        int cnt = 0;
        for (SeatStatusVO s : board.getSeats()) {
            if ("FREE".equals(s.getStatus())) cnt++;
        }
        return cnt;
    }

    public SeatwiseProps getProps() {
        return props;
    }

    /**
     * 历史回放：由当天已落地的预约（待签到/使用中/已完成）按时间片重建每一帧的座位占用。
     * 不新增事件表，直接以 reservation 为真源。
     */
    public ReplayVO buildReplay(Long roomId, LocalDate date) {
        int slotMin = props.getSlotMinutes();
        StudyRoom room = studyRoomMapper.selectById(roomId);
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getRoomId, roomId)
                .orderByAsc(Seat::getRowIndex).orderByAsc(Seat::getColIndex));

        List<Reservation> booked = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getRoomId, roomId)
                .eq(Reservation::getDate, date)
                .in(Reservation::getStatus, List.of("PENDING_SIGN_IN", "IN_USE", "COMPLETED")));

        int rows = 0, cols = 0, totalSeats = 0;
        List<ReplayVO.SeatMeta> metas = new ArrayList<>();
        for (Seat seat : seats) {
            rows = Math.max(rows, seat.getRowIndex() + 1);
            cols = Math.max(cols, seat.getColIndex() + 1);
            if ("SEAT".equals(seat.getCellType()) && seat.getEnabled() != null && seat.getEnabled() == 1) totalSeats++;
            ReplayVO.SeatMeta m = new ReplayVO.SeatMeta();
            m.setSeatId(seat.getId());
            m.setSeatNo(seat.getSeatNo());
            m.setRowIndex(seat.getRowIndex());
            m.setColIndex(seat.getColIndex());
            m.setCellType(seat.getCellType());
            m.setEnabled(seat.getEnabled());
            metas.add(m);
        }

        // 回放范围：优先自习室开放时段，否则 08:00-22:00
        int fromSlot = room != null && room.getOpenStart() != null
                ? SlotUtil.toSlot(room.getOpenStart(), slotMin) : (8 * 60) / slotMin;
        int toSlot = room != null && room.getOpenEnd() != null
                ? SlotUtil.toSlot(room.getOpenEnd(), slotMin) : (22 * 60) / slotMin;

        List<ReplayVO.Frame> timeline = new ArrayList<>();
        for (int slot = fromSlot; slot < toSlot; slot++) {
            List<Long> occ = new ArrayList<>();
            for (Reservation r : booked)
                if (r.getStartSlot() <= slot && slot < r.getEndSlot()) occ.add(r.getSeatId());
            ReplayVO.Frame f = new ReplayVO.Frame();
            f.setSlotIndex(slot);
            f.setLabel(SlotUtil.label(slot, slotMin));
            f.setOccupied(occ);
            f.setOccupiedCount(occ.size());
            timeline.add(f);
        }

        ReplayVO vo = new ReplayVO();
        vo.setRoomId(roomId);
        vo.setRoomName(room != null ? room.getName() : "");
        vo.setDate(date);
        vo.setRows(rows);
        vo.setCols(cols);
        vo.setTotalSeats(totalSeats);
        vo.setSeats(metas);
        vo.setTimeline(timeline);
        return vo;
    }
}
