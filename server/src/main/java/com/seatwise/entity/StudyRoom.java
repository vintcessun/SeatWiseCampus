package com.seatwise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("study_room")
public class StudyRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long buildingId;
    private Integer floorNo;
    private String name;
    private LocalTime openStart;
    private LocalTime openEnd;
    private String status;        // OPEN / CLOSED
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer mapX;
    private Integer mapY;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
