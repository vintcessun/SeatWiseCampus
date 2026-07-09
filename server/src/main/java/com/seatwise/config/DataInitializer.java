package com.seatwise.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.entity.Reservation;
import com.seatwise.entity.Seat;
import com.seatwise.entity.StudyRoom;
import com.seatwise.entity.User;
import com.seatwise.mapper.ReservationMapper;
import com.seatwise.mapper.SeatMapper;
import com.seatwise.mapper.StudyRoomMapper;
import com.seatwise.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 启动时若座位为空，则为每个自习室生成 6×8 座位网格（第 4 列为过道），
 * 保证演示环境有可视化的座位排布。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SeatMapper seatMapper;
    private final StudyRoomMapper roomMapper;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final PasswordEncoder passwordEncoder;

    private static final int ROWS = 6;
    private static final int COLS = 8;
    private static final int AISLE_COL = 3;

    @Override
    public void run(String... args) {
        migratePlainPasswords();
        Long count = seatMapper.selectCount(new LambdaQueryWrapper<>());
        if (count != null && count > 0) {
            return;
        }
        List<StudyRoom> rooms = roomMapper.selectList(new LambdaQueryWrapper<>());
        for (StudyRoom room : rooms) {
            generate(room.getId());
        }
        log.info("已为 {} 个自习室生成座位网格", rooms.size());
        seedHistory();
    }

    /**
     * 演示历史数据：为最近 7 天注入若干「已完成」预约（含少量爽约释放），
     * 使个人自习报告 / 历史回放 / 排行有真实可看的历史。仅在无任何预约时执行。
     */
    private void seedHistory() {
        Long resCount = reservationMapper.selectCount(new LambdaQueryWrapper<>());
        if (resCount != null && resCount > 0) return;

        StudyRoom room = roomMapper.selectList(new LambdaQueryWrapper<>()).stream().findFirst().orElse(null);
        if (room == null) return;
        Long roomId = room.getId();
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getRoomId, roomId).eq(Seat::getCellType, "SEAT").eq(Seat::getEnabled, 1)
                .orderByAsc(Seat::getRowIndex).orderByAsc(Seat::getColIndex));
        List<User> students = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "STUDENT").orderByAsc(User::getId));
        if (seats.isEmpty() || students.isEmpty()) return;

        // 每天时段模板（slotIndex 对，30 分钟片）：8:00-10:00 / 10:00-12:00 / 14:00-16:00 / 19:00-21:00
        int[][] windows = {{16, 20}, {20, 24}, {28, 32}, {38, 42}};
        int created = 0, seatIdx = 0;
        for (int d = 1; d <= 7; d++) {
            LocalDate date = LocalDate.now().minusDays(d);
            int sessions = 3 + (d % 2); // 3~4 场/天
            for (int s = 0; s < sessions; s++) {
                User u = students.get((d * 3 + s) % students.size());
                Seat seat = seats.get(seatIdx++ % seats.size());
                int[] w = windows[(d + s) % windows.length];
                boolean noShow = (d == 2 && s == 0) || (d == 5 && s == 1); // 少量爽约
                Reservation r = new Reservation();
                r.setUserId(u.getId());
                r.setSeatId(seat.getId());
                r.setRoomId(roomId);
                r.setDate(date);
                r.setStartSlot(w[0]);
                r.setEndSlot(w[1]);
                if (noShow) {
                    r.setStatus("EXPIRED_RELEASED");
                } else {
                    r.setStatus("COMPLETED");
                    r.setCheckInTime(LocalDateTime.of(date, LocalTime.of(w[0] / 2, (w[0] % 2) * 30)));
                    r.setCheckOutTime(LocalDateTime.of(date, LocalTime.of(w[1] / 2, (w[1] % 2) * 30)));
                }
                reservationMapper.insert(r);
                created++;
            }
        }
        log.info("已注入 {} 条演示历史预约（最近 7 天）", created);
    }

    /** 将种子数据里的明文密码升级为 BCrypt（幂等：已是 $2 前缀的跳过） */
    private void migratePlainPasswords() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());
        int migrated = 0;
        for (User u : users) {
            String pwd = u.getPassword();
            if (pwd != null && !pwd.startsWith("$2")) {
                u.setPassword(passwordEncoder.encode(pwd));
                userMapper.updateById(u);
                migrated++;
            }
        }
        if (migrated > 0) log.info("已将 {} 个用户的明文密码升级为 BCrypt", migrated);
    }

    private void generate(Long roomId) {
        for (int r = 0; r < ROWS; r++) {
            int seq = 1;
            char rowLetter = (char) ('A' + r);
            for (int c = 0; c < COLS; c++) {
                Seat seat = new Seat();
                seat.setRoomId(roomId);
                seat.setRowIndex(r);
                seat.setColIndex(c);
                if (c == AISLE_COL) {
                    seat.setCellType("AISLE");
                    seat.setEnabled(1);
                } else {
                    seat.setCellType("SEAT");
                    seat.setSeatNo(rowLetter + "-" + String.format("%02d", seq++));
                    // 演示：每个房间禁用第一排最后一个座位，展示 DISABLED
                    seat.setEnabled((r == 0 && c == COLS - 1) ? 0 : 1);
                }
                seatMapper.insert(seat);
            }
        }
    }
}
