package com.seatwise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seat")
public class Seat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Integer rowIndex;
    private Integer colIndex;
    private String cellType;      // SEAT / AISLE / EMPTY / DISABLED
    private String seatNo;
    private Integer enabled;      // 1 启用 / 0 禁用
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
