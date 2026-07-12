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
    // updateStrategy=ALWAYS：清空标签（置 null）时也要写回，否则 updateById 默认跳过 null 字段
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tags;          // 逗号分隔座位属性，如 window,power
    private Integer enabled;      // 1 启用 / 0 禁用
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
