-- SeatWise Campus 数据库结构
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS seatwise DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE seatwise;

DROP TABLE IF EXISTS reservation_slot;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS seat;
DROP TABLE IF EXISTS study_room;
DROP TABLE IF EXISTS building;
DROP TABLE IF EXISTS campus;
DROP TABLE IF EXISTS blacklist_record;
DROP TABLE IF EXISTS score_record;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64),
    role VARCHAR(16) NOT NULL DEFAULT 'STUDENT',
    credit_score INT NOT NULL DEFAULT 0,
    no_show_count INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_time DATETIME,
    updated_time DATETIME,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE campus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    map_x INT,
    map_y INT,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_time DATETIME,
    updated_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE building (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campus_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    map_x INT,
    map_y INT,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_time DATETIME,
    updated_time DATETIME,
    KEY idx_campus (campus_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE study_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    building_id BIGINT NOT NULL,
    floor_no INT NOT NULL,
    name VARCHAR(64) NOT NULL,
    open_start TIME NOT NULL DEFAULT '08:00:00',
    open_end TIME NOT NULL DEFAULT '22:00:00',
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    map_x INT,
    map_y INT,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_time DATETIME,
    updated_time DATETIME,
    KEY idx_building_floor (building_id, floor_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    row_index INT NOT NULL,
    col_index INT NOT NULL,
    cell_type VARCHAR(16) NOT NULL DEFAULT 'SEAT',
    seat_no VARCHAR(16),
    enabled TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_time DATETIME,
    updated_time DATETIME,
    KEY idx_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    date DATE NOT NULL,
    start_slot INT NOT NULL,
    end_slot INT NOT NULL,
    status VARCHAR(24) NOT NULL,
    check_in_time DATETIME,
    check_out_time DATETIME,
    created_time DATETIME,
    updated_time DATETIME,
    KEY idx_user_date (user_id, date),
    KEY idx_room_date (room_id, date),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 时间片占用：唯一索引是防重复预约的最终兜底
CREATE TABLE reservation_slot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    date DATE NOT NULL,
    slot_index INT NOT NULL,
    created_time DATETIME,
    UNIQUE KEY uk_seat_date_slot (seat_id, date, slot_index),
    KEY idx_reservation (reservation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE blacklist_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    reason VARCHAR(128),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    active TINYINT NOT NULL DEFAULT 1,
    created_time DATETIME,
    updated_time DATETIME,
    KEY idx_user_active (user_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(24) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(512),
    read_flag TINYINT NOT NULL DEFAULT 0,
    created_time DATETIME,
    KEY idx_user_read (user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE score_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    score_change INT NOT NULL,
    reason VARCHAR(32) NOT NULL,
    ref_reservation_id BIGINT,
    created_time DATETIME,
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
