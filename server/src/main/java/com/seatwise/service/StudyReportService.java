package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.entity.Reservation;
import com.seatwise.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 个人自习报告：纯聚合既有 reservation，无需建表。
 * 累计场次/时长、守约率、连续自习天数（streak）、近 7 天时长。
 */
@Service
@RequiredArgsConstructor
public class StudyReportService {

    private final ReservationMapper reservationMapper;
    private final SeatwiseProps props;

    public Map<String, Object> report(Long userId) {
        int slotMin = props.getSlotMinutes();
        List<Reservation> all = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId));

        int completed = 0, expired = 0;
        double totalHours = 0;
        Set<LocalDate> completedDays = new HashSet<>();
        for (Reservation r : all) {
            if ("COMPLETED".equals(r.getStatus())) {
                completed++;
                totalHours += (r.getEndSlot() - r.getStartSlot()) * slotMin / 60.0;
                if (r.getDate() != null) completedDays.add(r.getDate());
            } else if ("EXPIRED_RELEASED".equals(r.getStatus())) {
                expired++;
            }
        }
        int onTimeRate = (completed + expired) == 0 ? 100
                : (int) Math.round(completed * 100.0 / (completed + expired));

        // streak：从最近有完成记录的一天起，向前连续的自然天数
        int streak = 0;
        if (!completedDays.isEmpty()) {
            LocalDate cursor = Collections.max(completedDays);
            while (completedDays.contains(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            }
        }

        // 近 7 天（含今天）每天完成时长
        List<Map<String, Object>> weekly = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            double h = 0;
            for (Reservation r : all)
                if ("COMPLETED".equals(r.getStatus()) && day.equals(r.getDate()))
                    h += (r.getEndSlot() - r.getStartSlot()) * slotMin / 60.0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", day.toString());
            m.put("hours", Math.round(h * 10) / 10.0);
            weekly.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("completedSessions", completed);
        res.put("expiredSessions", expired);
        res.put("totalSessions", all.size());
        res.put("totalHours", Math.round(totalHours * 10) / 10.0);
        res.put("onTimeRate", onTimeRate);
        res.put("streakDays", streak);
        res.put("weekly", weekly);
        return res;
    }
}
