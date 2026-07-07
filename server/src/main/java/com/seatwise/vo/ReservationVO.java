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
}
