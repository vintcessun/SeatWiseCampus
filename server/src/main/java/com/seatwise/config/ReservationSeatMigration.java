package com.seatwise.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.entity.Reservation;
import com.seatwise.entity.Seat;
import com.seatwise.mapper.ReservationMapper;
import com.seatwise.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动迁移：修正已有预约记录的 seat_id。
 * 若 reservation.seat_id 指向不存在的座位，则从同自习室中取一个有效且未分配给其他预约的座位补上。
 */
@Slf4j
@Component
@Order(999)
@RequiredArgsConstructor
public class ReservationSeatMigration implements CommandLineRunner {

    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;

    @Override
    public void run(String... args) {
        List<Reservation> all = reservationMapper.selectList(new LambdaQueryWrapper<>());
        int fixed = 0;
        for (Reservation r : all) {
            if (r.getSeatId() == null) continue;
            Seat seat = seatMapper.selectById(r.getSeatId());
            if (seat != null) continue;

            // seat_id 无效，从同自习室找一个 SEAT 类型且启用的座位
            List<Seat> candidates = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                    .eq(Seat::getRoomId, r.getRoomId())
                    .eq(Seat::getCellType, "SEAT")
                    .eq(Seat::getEnabled, 1)
                    .last("limit 1"));
            if (candidates.isEmpty()) {
                log.warn("自习室 {} 无有效座位，跳过预约 {}", r.getRoomId(), r.getId());
                continue;
            }
            Seat replacement = candidates.get(0);
            log.info("预约 {} seat_id 修正为 {}（原 {} 不存在）", r.getId(), replacement.getId(), r.getSeatId());
            r.setSeatId(replacement.getId());
            reservationMapper.updateById(r);
            fixed++;
        }
        if (fixed > 0) log.info("预约 seat_id 迁移完成，共修正 {} 条", fixed);
    }
}
