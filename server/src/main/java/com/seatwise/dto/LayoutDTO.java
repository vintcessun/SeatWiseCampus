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
        private Integer rowIndex;
        private Integer colIndex;
        private String cellType;   // SEAT / AISLE / EMPTY / DISABLED
        private String seatNo;
        private Integer enabled;
    }
}
