package com.seatwise.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservationVO {
    private Long id;
    private Long seatId;
    private String seatNo;
    private Long roomId;
    private String roomName;
    private String buildingName;
    private LocalDate date;
    private String startTime;
    private String endTime;
    private String status;
    private String checkInTime;
    private String checkOutTime;
    private String signinStart;      // 签到开放时间（=预约开始）
    private String signinDeadline;   // 签到截止时间（=开始+窗口）
    private Integer scoreDelta;      // 本次操作的积分变化（签退/取消返回）
    private String studentName;      // 管理端追踪用
    private String username;         // 管理端追踪用
}
