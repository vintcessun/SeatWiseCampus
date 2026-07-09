package com.seatwise.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务参数（对应 server/11 环境变量）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "seatwise")
public class SeatwiseProps {
    /** 时间片粒度（分钟） */
    private int slotMinutes = 30;
    /** 签到窗口（分钟） */
    private int signinWindowMinutes = 15;
    /** 爽约阈值 */
    private int noshowThreshold = 3;
    /** 黑名单期限（天） */
    private int blacklistDays = 7;
    /** 单日预约次数上限 */
    private int dailyLimit = 3;
    /** 单次预约最大时间片数 */
    private int maxSlotsPerReservation = 8;
    /** 临时锁座保留秒数 */
    private int holdSeconds = 90;
    /** 组队相邻预约单次最多座位数 */
    private int groupMaxSeats = 6;
    /** 预约开始前多少分钟推送「即将开始」提醒 */
    private int remindBeforeMinutes = 30;
}
