package com.seatwise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("score_record")
public class ScoreRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    @TableField("score_change")
    private Integer scoreChange;
    private String reason;        // CHECKOUT_OK/CANCEL_LATE/NO_SHOW/NO_CHECKOUT
    private Long refReservationId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
