package com.seatwise.vo;

import lombok.Data;

@Data
public class SeatStatusVO {
    private Long seatId;
    private String seatNo;
    private Integer rowIndex;
    private Integer colIndex;
    private String cellType;   // SEAT / AISLE / EMPTY / DISABLED
    private String tags;       // 逗号分隔座位属性，如 window,power
    private String status;     // FREE / RESERVED / USING / DISABLED / HELD
    private Boolean mine;      // 当前用户是否预约了该座位
    private Long heldBy;       // 临时锁座持有者 userId（HELD 时）
    private Long holdExpireAt; // 锁座到期时间（epoch ms，HELD 时）
}
