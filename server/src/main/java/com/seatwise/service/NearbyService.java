package com.seatwise.service;

import com.seatwise.common.BizError;
import com.seatwise.common.BizException;
import com.seatwise.common.SlotUtil;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.entity.Building;
import com.seatwise.entity.StudyRoom;
import com.seatwise.mapper.BuildingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * 最近空位推荐：MVP 手动选当前位置（楼栋），按 同楼栋 > 距离最近 > 空位更多 排序，
 * 且不返回无空位/未开放的自习室。
 */
@Service
@RequiredArgsConstructor
public class NearbyService {

    private final BaseDataService baseDataService;
    private final BuildingMapper buildingMapper;
    private final BoardService boardService;
    private final SeatwiseProps props;

    public List<Map<String, Object>> nearestAvailable(Long originBuildingId, LocalDate date,
                                                       LocalTime start, LocalTime end) {
        if (originBuildingId == null) throw new BizException(BizError.GEO_LOCATION_REQUIRED);
        Building origin = buildingMapper.selectById(originBuildingId);
        if (origin == null) throw new BizException(BizError.GEO_LOCATION_REQUIRED);

        int slotMin = props.getSlotMinutes();
        int startSlot = SlotUtil.toSlot(start, slotMin);
        int endSlot = SlotUtil.toSlot(end, slotMin);

        List<StudyRoom> rooms = baseDataService.listRooms(origin.getCampusId(), null, null);
        List<Map<String, Object>> list = new ArrayList<>();
        for (StudyRoom room : rooms) {
            if (!"OPEN".equalsIgnoreCase(room.getStatus() == null ? "OPEN" : room.getStatus())) continue;
            int available = boardService.countAvailable(room.getId(), date, startSlot, endSlot);
            if (available <= 0) continue;
            Building b = buildingMapper.selectById(room.getBuildingId());
            boolean sameBuilding = room.getBuildingId().equals(originBuildingId);
            double distance = sameBuilding ? 0 : distance(origin, b);
            Map<String, Object> m = new HashMap<>();
            m.put("roomId", room.getId());
            m.put("roomName", room.getName());
            m.put("buildingName", b != null ? b.getName() : "");
            m.put("floorNo", room.getFloorNo());
            m.put("sameBuilding", sameBuilding);
            m.put("distance", Math.round(distance * 10) / 10.0);
            m.put("availableSeats", available);
            m.put("open", true);
            list.add(m);
        }
        if (list.isEmpty()) throw new BizException(BizError.NO_AVAILABLE_ROOM_NEARBY);

        list.sort((a, x) -> {
            int c = Boolean.compare((boolean) x.get("sameBuilding"), (boolean) a.get("sameBuilding"));
            if (c != 0) return c;
            c = Double.compare((double) a.get("distance"), (double) x.get("distance"));
            if (c != 0) return c;
            return (int) x.get("availableSeats") - (int) a.get("availableSeats");
        });
        return list;
    }

    private double distance(Building a, Building b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        Integer ax = a.getMapX(), ay = a.getMapY(), bx = b.getMapX(), by = b.getMapY();
        if (ax != null && ay != null && bx != null && by != null) {
            return Math.hypot(ax - bx, ay - by);
        }
        BigDecimal alat = a.getLatitude(), alng = a.getLongitude(), blat = b.getLatitude(), blng = b.getLongitude();
        if (alat != null && alng != null && blat != null && blng != null) {
            double dLat = alat.doubleValue() - blat.doubleValue();
            double dLng = alng.doubleValue() - blng.doubleValue();
            return Math.hypot(dLat, dLng) * 100000;
        }
        return 9999;
    }
}
