package com.seatwise.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BoardVO {
    private Long roomId;
    private String roomName;
    private LocalDate date;
    private Integer rows;
    private Integer cols;
    private Integer startSlot;
    private Integer endSlot;
    private List<SeatStatusVO> seats;
    private String features;      // JSON 覆盖层（门/讲台）
}
