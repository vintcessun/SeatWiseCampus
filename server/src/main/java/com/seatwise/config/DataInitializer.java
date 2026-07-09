package com.seatwise.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.entity.Seat;
import com.seatwise.entity.StudyRoom;
import com.seatwise.entity.User;
import com.seatwise.mapper.SeatMapper;
import com.seatwise.mapper.StudyRoomMapper;
import com.seatwise.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
