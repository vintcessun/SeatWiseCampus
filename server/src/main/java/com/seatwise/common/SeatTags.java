package com.seatwise.common;

import com.seatwise.entity.Seat;
import com.seatwise.entity.StudyRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * 从座位排布与房间信息推导座位属性标签（演示用，确定性派生，无需额外建表）。
 * 标签用于「可解释的多目标推荐」。
 */
public final class SeatTags {
    private SeatTags() {}

    public static final String WINDOW = "window";   // 靠窗
    public static final String POWER = "power";      // 有插座
    public static final String QUIET = "quiet";      // 安静区
    public static final String DISCUSS = "discuss";  // 讨论区
    public static final String AISLE_NEAR = "near_door"; // 靠门

    public static List<String> of(Seat seat, StudyRoom room, int cols) {
        List<String> tags = new ArrayList<>();
        if (seat.getColIndex() != null && (seat.getColIndex() == 0 || seat.getColIndex() == cols - 1)) {
            tags.add(WINDOW);
        }
        if (seat.getRowIndex() != null && seat.getRowIndex() % 2 == 0) {
            tags.add(POWER);
        }
        if (room != null && room.getName() != null) {
            if (room.getName().contains("静音") || room.getName().contains("考研")) tags.add(QUIET);
            if (room.getName().contains("讨论")) tags.add(DISCUSS);
        }
        if (seat.getRowIndex() != null && seat.getRowIndex() == 0) {
            tags.add(AISLE_NEAR);
        }
        return tags;
    }

    public static String cn(String tag) {
        return switch (tag) {
            case WINDOW -> "靠窗";
            case POWER -> "有插座";
            case QUIET -> "安静区";
            case DISCUSS -> "讨论区";
            case AISLE_NEAR -> "靠门";
            default -> tag;
        };
    }
}
