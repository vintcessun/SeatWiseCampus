package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.common.SlotUtil;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.entity.Reservation;
import com.seatwise.entity.StudyRoom;
import com.seatwise.mapper.ReservationMapper;
import com.seatwise.mapper.StudyRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 报表：演示场景数据量小，直接从 reservation 实时聚合。
 * 生产环境应改为 room_daily_stats 聚合表（见 server/08）。
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReservationMapper reservationMapper;
    private final StudyRoomMapper roomMapper;
    private final SeatwiseProps props;

    public Map<String, Object> summary() {
        List<Reservation> all = reservationMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, Object> result = new HashMap<>();

        // 状态分布（饼图）
        Map<String, Integer> statusCount = new LinkedHashMap<>();
        for (String st : List.of("PENDING_SIGN_IN", "IN_USE", "COMPLETED", "CANCELLED", "EXPIRED_RELEASED")) {
            statusCount.put(st, 0);
        }
        for (Reservation r : all) statusCount.merge(r.getStatus(), 1, Integer::sum);
        result.put("statusDistribution", statusCount);

        int total = all.size();
        int cancelled = statusCount.getOrDefault("CANCELLED", 0);
        int noShow = statusCount.getOrDefault("EXPIRED_RELEASED", 0);
        result.put("total", total);
        result.put("cancelRate", rate(cancelled, total));
        result.put("noShowRate", rate(noShow, total));

        // 热门时段（按 startSlot 统计）
        Map<Integer, Integer> slotCount = new TreeMap<>();
        for (Reservation r : all) slotCount.merge(r.getStartSlot(), 1, Integer::sum);
        List<Map<String, Object>> peak = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : slotCount.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("slotIndex", e.getKey());
            m.put("timeLabel", SlotUtil.label(e.getKey(), props.getSlotMinutes()));
            m.put("count", e.getValue());
            peak.add(m);
        }
        result.put("peakSlots", peak);

        // 自习室利用率排行（按预约数）
        Map<Long, Integer> roomCount = new HashMap<>();
        for (Reservation r : all) roomCount.merge(r.getRoomId(), 1, Integer::sum);
        List<StudyRoom> rooms = roomMapper.selectList(new LambdaQueryWrapper<>());
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (StudyRoom room : rooms) {
            Map<String, Object> m = new HashMap<>();
            m.put("roomId", room.getId());
            m.put("roomName", room.getName());
            m.put("reservationCount", roomCount.getOrDefault(room.getId(), 0));
            ranking.add(m);
        }
        ranking.sort((a, b) -> (int) b.get("reservationCount") - (int) a.get("reservationCount"));
        int rank = 1;
        for (Map<String, Object> m : ranking) m.put("rank", rank++);
        result.put("roomRanking", ranking);

        return result;
    }

    private double rate(int part, int total) {
        if (total == 0) return 0.0;
        return Math.round(part * 10000.0 / total) / 100.0;
    }
}
