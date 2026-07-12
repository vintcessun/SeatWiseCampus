package com.seatwise.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动迁移：给 study_room 表加 features 字段（门/讲台覆盖层）。
 * 无 API/数据影响；已在 study_room 实体上加 features 字段。
 * 用户指令："门/讲台考虑一种持久化方案"。
 */
@Slf4j
@Component
@Order(998)
@RequiredArgsConstructor
public class StudyRoomFeaturesMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE study_room ADD COLUMN features TEXT COMMENT 'JSON覆盖层' AFTER `status`");
            log.info("study_room.features 字段已添加");
        } catch (Exception e) {
            log.info("study_room.features 字段已存在，跳过");
        }
    }
}
