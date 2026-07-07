package com.seatwise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blacklist_record")
public class BlacklistRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer active;       // 1 有效 / 0 失效
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
