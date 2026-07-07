package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.entity.*;
import com.seatwise.mapper.*;
import com.seatwise.vo.BoardVO;
import com.seatwise.vo.SeatStatusVO;
import lombok.RequiredArgsConstructor;
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
}
