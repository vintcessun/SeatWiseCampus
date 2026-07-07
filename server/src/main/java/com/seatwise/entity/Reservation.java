package com.seatwise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long seatId;
    private Long roomId;
    private LocalDate date;
    private Integer startSlot;
    private Integer endSlot;
    private String status;        // PENDING_SIGN_IN/IN_USE/COMPLETED/CANCELLED/EXPIRED_RELEASED
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
