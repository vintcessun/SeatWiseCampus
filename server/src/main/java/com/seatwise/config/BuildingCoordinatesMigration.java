package com.seatwise.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 启动迁移：将所有校区和楼栋的经纬度设为默认坐标 (24.605422, 118.313908)。
 * 新装数据库通过 SQL seed 已生效，此迁移覆盖已有部署中的旧数据。
 */
@Slf4j
@Component
@Order(997)
@RequiredArgsConstructor
public class BuildingCoordinatesMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final BigDecimal DEFAULT_LAT = new BigDecimal("24.605422");
    private static final BigDecimal DEFAULT_LNG = new BigDecimal("118.313908");

    @Override
    public void run(String... args) {
        int campusUpdated = jdbcTemplate.update(
            "UPDATE campus SET latitude = ?, longitude = ? WHERE latitude IS NULL OR latitude <> ?",
            DEFAULT_LAT, DEFAULT_LNG, DEFAULT_LAT);
        if (campusUpdated > 0) log.info("校区坐标已更新 {} 条", campusUpdated);

        int buildingUpdated = jdbcTemplate.update(
            "UPDATE building SET latitude = ?, longitude = ? WHERE latitude IS NULL OR latitude <> ?",
            DEFAULT_LAT, DEFAULT_LNG, DEFAULT_LAT);
        if (buildingUpdated > 0) log.info("楼栋坐标已更新 {} 条", buildingUpdated);
    }
}
