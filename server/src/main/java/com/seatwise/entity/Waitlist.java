package com.seatwise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("waitlist")
public class Waitlist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roomId;
    private LocalDate date;
    private Integer startSlot;
    private Integer endSlot;
    private String status;          // WAITING/OFFERED/FULFILLED/CANCELLED/EXPIRED
    private Long offeredSeatId;
    private LocalDateTime offerExpireAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
