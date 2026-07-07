package com.seatwise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时间片占用记录。唯一索引 (seat_id, date, slot_index) 是防重复预约的最终兜底。
 */
@Data
@TableName("reservation_slot")
public class ReservationSlot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reservationId;
    private Long seatId;
    private LocalDate date;
    private Integer slotIndex;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
