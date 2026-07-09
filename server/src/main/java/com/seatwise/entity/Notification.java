package com.seatwise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;      // SCORE / BLACKLIST / RESERVATION / SYSTEM
    private String title;
    private String content;
    private Integer readFlag; // 0 未读 / 1 已读
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
