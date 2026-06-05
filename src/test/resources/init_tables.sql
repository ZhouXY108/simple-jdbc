-- 测试用户表，覆盖全部列类型（数值/字符串/布尔/时间），用于验证 RowMapper 和查询功能
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    age INT,
    balance BIGINT,
    active BOOLEAN,
    created_at TIMESTAMP,
    birth_date DATE,
    work_start_time TIME
);

-- 唯一约束用于测试 batchUpdate 的重复键错误场景
ALTER TABLE users ADD CONSTRAINT uk_username UNIQUE (username);

-- 完整数据行（全部字段非空）
INSERT INTO users (username, email, age, balance, active, created_at, birth_date, work_start_time)
VALUES ('alice', 'alice@example.com', 28, 15000, TRUE, '2024-01-15 09:30:00', '1996-05-20', '08:00:00');

-- 完整数据行
INSERT INTO users (username, email, age, balance, active, created_at, birth_date, work_start_time)
VALUES ('bob', 'bob@example.com', 35, 25000, TRUE, '2023-11-01 14:00:00', '1989-03-12', '09:30:00');

-- null 数据行：email、age、birth_date、work_start_time 均为 null，用于测试空字段映射
INSERT INTO users (username, email, age, balance, active, created_at, birth_date, work_start_time)
VALUES ('charlie', NULL, NULL, 5000, FALSE, '2025-03-10 11:15:00', NULL, NULL);

-- 部分 null 数据行：balance 为 null，created_at 为 null，用于测试字段级 null 映射
INSERT INTO users (username, email, age, balance, active, created_at, birth_date, work_start_time)
VALUES ('diana', 'diana@example.com', 42, NULL, TRUE, NULL, '1982-11-08', '07:45:00');

-- 完整数据行
INSERT INTO users (username, email, age, balance, active, created_at, birth_date, work_start_time)
VALUES ('eve', 'eve@example.com', 31, 8000, TRUE, '2024-07-22 16:45:00', '1993-01-30', '10:00:00');
