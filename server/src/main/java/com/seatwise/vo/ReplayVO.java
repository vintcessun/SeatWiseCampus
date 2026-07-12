package com.seatwise.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 座位历史回放：由 reservation 记录按时间片重建，无需事件表。
 */
@Data
public class ReplayVO {
    private Long roomId;
    private String roomName;
    private LocalDate date;
    private Integer rows;
    private Integer cols;
    private Integer totalSeats;     // 可预约座位总数
    private List<SeatMeta> seats;   // 座位静态信息（含过道/禁用，供网格渲染）
    private List<Frame> timeline;   // 每个时间片一帧
    private String features;        // JSON 覆盖层（门/讲台）

    @Data
    public static class SeatMeta {
        private Long seatId;
        private String seatNo;
        private Integer rowIndex;
        private Integer colIndex;
        private String cellType;    // SEAT / AISLE / EMPTY / DISABLED
        private Integer enabled;
    }

    @Data
    public static class Frame {
        private Integer slotIndex;
        private String label;        // HH:mm
        private List<Long> occupied; // 该时间片被占用的 seatId
        private Integer occupiedCount;
    }
}
