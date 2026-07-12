-- SeatWise Campus 种子数据（演示用，密码为明文，仅演示）
SET NAMES utf8mb4;
USE seatwise;

INSERT INTO sys_user (username, password, real_name, role, credit_score, no_show_count, created_time, updated_time) VALUES
('admin',    'admin123', '系统管理员', 'ADMIN',   0, 0, NOW(), NOW()),
('student1', '123456',   '张三',       'STUDENT', 12, 0, NOW(), NOW()),
('student2', '123456',   '李四',       'STUDENT', 8,  0, NOW(), NOW()),
('student3', '123456',   '王五',       'STUDENT', 5,  0, NOW(), NOW()),
('student4', '123456',   '赵六',       'STUDENT', 2,  2, NOW(), NOW()),
('student5', '123456',   '钱七',       'STUDENT', 0,  0, NOW(), NOW()),
('student6', '123456',   '孙八',       'STUDENT', 0,  0, NOW(), NOW()),
('student7', '123456',   '周九',       'STUDENT', 0,  0, NOW(), NOW()),
('student8', '123456',   '吴十',       'STUDENT', 0,  0, NOW(), NOW());

INSERT INTO campus (id, name, latitude, longitude, map_x, map_y, created_time, updated_time) VALUES
(1, '中心校区', 24.605422, 118.313908, 0, 0, NOW(), NOW());

INSERT INTO building (id, campus_id, name, latitude, longitude, map_x, map_y, created_time, updated_time) VALUES
(1, 1, '图书馆A座', 24.605422, 118.313908, 20,  30,  NOW(), NOW()),
(2, 1, '第三教学楼', 24.605422, 118.313908, 160, 90,  NOW(), NOW());

INSERT INTO study_room (id, building_id, floor_no, name, open_start, open_end, status, map_x, map_y, created_time, updated_time) VALUES
(1, 1, 3, 'A301 静音自习室', '08:00:00', '22:00:00', 'OPEN', 20,  30,  NOW(), NOW()),
(2, 1, 3, 'A302 讨论自习室', '08:00:00', '22:00:00', 'OPEN', 25,  35,  NOW(), NOW()),
(3, 2, 2, 'C201 考研自习室', '08:00:00', '22:00:00', 'OPEN', 160, 90,  NOW(), NOW());

INSERT INTO announcement (title, content, level, active, publisher_id, created_time, updated_time) VALUES
('期末考试季·延长开放时间', '即日起至考试周结束，全部自习室开放时间延长至 23:00，请合理安排、按时签到。', 'INFO', 1, 1, NOW(), NOW()),
('本周五下午闭馆维护', '本周五 14:00-18:00 图书馆A座进行电路维护，A301/A302 暂停开放，已预约将自动释放，敬请谅解。', 'WARN', 1, 1, NOW(), NOW());
