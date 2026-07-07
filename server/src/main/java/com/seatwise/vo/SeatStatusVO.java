package com.seatwise.vo;

import lombok.Data;

@Data
public class SeatStatusVO {
    private Long seatId;
    private String seatNo;
    private Integer rowIndex;
    private Integer colIndex;
    private String cellType;   // SEAT / AISLE / EMPTY / DISABLED
    private String status;     // FREE / RESERVED / USING / DISABLED
    private Boolean mine;      // 当前用户是否预约了该座位
}
