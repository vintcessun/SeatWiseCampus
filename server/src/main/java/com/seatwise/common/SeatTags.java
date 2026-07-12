package com.seatwise.common;

import com.seatwise.entity.Seat;
import com.seatwise.entity.StudyRoom;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /** 合法标签集合（内部使用，不对外暴露常量列表）。 */
    private static final Set<String> VALID = Set.of(WINDOW, POWER, QUIET, DISCUSS, AISLE_NEAR);

    /** 将逗号分隔 CSV 解析为合法标签列表（去空白、过滤非法值、保序去重）。 */
    public static List<String> parse(String csv) {
        List<String> result = new ArrayList<>();
        if (csv == null || csv.isBlank()) return result;
        Set<String> seen = new LinkedHashSet<>();
        for (String raw : csv.split(",")) {
            String t = raw.trim();
            if (VALID.contains(t)) seen.add(t);
        }
        result.addAll(seen);
        return result;
    }

    /** 将标签列表拼成 CSV（去重、按合法集合过滤）；空则返回 null。 */
    public static String join(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        Set<String> seen = new LinkedHashSet<>();
        for (String raw : tags) {
            if (raw == null) continue;
            String t = raw.trim();
            if (VALID.contains(t)) seen.add(t);
        }
        return seen.isEmpty() ? null : String.join(",", seen);
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
