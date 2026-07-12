package com.seatwise.dto;

import lombok.Data;

import java.util.List;

@Data
public class LayoutDTO {
    private Integer rows;
    private Integer cols;
    private List<Cell> cells;

    @Data
    public static class Cell {
        private Long seatId;       // 已有座位则携带，用于 upsert 判定；新增为 null
        private Integer rowIndex;
        private Integer colIndex;
        private String cellType;   // SEAT / AISLE / EMPTY / DISABLED
        private String seatNo;
        private String tags;       // 逗号分隔座位属性，如 window,power
        private Integer enabled;
    }
}
