package com.seatwise.common;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间片换算：slotIndex = (hour*60+minute)/slotMinutes，从 00:00 起。
 * 例如 slotMinutes=30 时 14:00 -> 28，16:00 -> 32（endSlot 不含）。
 */
public final class SlotUtil {
    private SlotUtil() {}

    public static int toSlot(LocalTime time, int slotMinutes) {
        return (time.getHour() * 60 + time.getMinute()) / slotMinutes;
    }

    public static LocalTime slotToTime(int slot, int slotMinutes) {
        int minutes = slot * slotMinutes;
        return LocalTime.of((minutes / 60) % 24, minutes % 60);
    }

    /** 展开 [startSlot, endSlot) 为 slotIndex 列表 */
    public static List<Integer> expand(int startSlot, int endSlot) {
        List<Integer> list = new ArrayList<>();
        for (int i = startSlot; i < endSlot; i++) {
            list.add(i);
        }
        return list;
    }

    public static String label(int slot, int slotMinutes) {
        LocalTime t = slotToTime(slot, slotMinutes);
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }
}
