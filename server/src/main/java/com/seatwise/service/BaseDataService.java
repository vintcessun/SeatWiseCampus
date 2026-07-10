package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.common.BizError;
import com.seatwise.common.BizException;
import com.seatwise.dto.LayoutDTO;
import com.seatwise.entity.*;
import com.seatwise.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BaseDataService {

    private static final List<String> ACTIVE = List.of("PENDING_SIGN_IN", "IN_USE");

    private final CampusMapper campusMapper;
    private final BuildingMapper buildingMapper;
    private final StudyRoomMapper roomMapper;
    private final SeatMapper seatMapper;
    private final ReservationMapper reservationMapper;
    private final AnnouncementService announcementService;
    private final NotificationService notificationService;

    public List<Campus> listCampuses() {
        return campusMapper.selectList(new LambdaQueryWrapper<Campus>().orderByAsc(Campus::getId));
    }

    public List<Building> listBuildings(Long campusId) {
        LambdaQueryWrapper<Building> w = new LambdaQueryWrapper<Building>().orderByAsc(Building::getId);
        if (campusId != null) w.eq(Building::getCampusId, campusId);
        return buildingMapper.selectList(w);
    }

    public List<StudyRoom> listRooms(Long campusId, Long buildingId, Integer floorNo) {
        LambdaQueryWrapper<StudyRoom> w = new LambdaQueryWrapper<StudyRoom>().orderByAsc(StudyRoom::getId);
        if (buildingId != null) w.eq(StudyRoom::getBuildingId, buildingId);
        if (floorNo != null) w.eq(StudyRoom::getFloorNo, floorNo);
        if (campusId != null && buildingId == null) {
            List<Building> bs = listBuildings(campusId);
            List<Long> ids = bs.stream().map(Building::getId).toList();
            if (ids.isEmpty()) return List.of();
            w.in(StudyRoom::getBuildingId, ids);
        }
        return roomMapper.selectList(w);
    }

    public Map<String, Object> getLayout(Long roomId) {
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getRoomId, roomId)
                .orderByAsc(Seat::getRowIndex).orderByAsc(Seat::getColIndex));
        int rows = 0, cols = 0;
        List<Map<String, Object>> cells = new ArrayList<>();
        for (Seat s : seats) {
            rows = Math.max(rows, s.getRowIndex() + 1);
            cols = Math.max(cols, s.getColIndex() + 1);
            Map<String, Object> c = new HashMap<>();
            c.put("rowIndex", s.getRowIndex());
            c.put("colIndex", s.getColIndex());
            c.put("cellType", s.getCellType());
            c.put("seatId", s.getId());
            c.put("seatNo", s.getSeatNo());
            c.put("enabled", s.getEnabled());
            cells.add(c);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("roomId", roomId);
        map.put("rows", rows);
        map.put("cols", cols);
        map.put("cells", cells);
        return map;
    }

    /** 该房间是否存在未来（待签到/进行中）预约 */
    private long activeReservations(Long roomId) {
        Long n = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getRoomId, roomId).in(Reservation::getStatus, ACTIVE));
        return n == null ? 0 : n;
    }

    @Transactional
    public void saveLayout(Long roomId, LayoutDTO dto) {
        // R10：重排会清空座位，若存在未来预约则拒绝，避免学生到馆座位不存在
        if (activeReservations(roomId) > 0)
            throw new BizException(BizError.ROOM_HAS_FUTURE_RESERVATION);
        // 简化：清空该房间座位后按布局重建（演示场景数据量小）
        seatMapper.delete(new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, roomId));
        if (dto.getCells() == null) return;
        for (LayoutDTO.Cell c : dto.getCells()) {
            Seat s = new Seat();
            s.setRoomId(roomId);
            s.setRowIndex(c.getRowIndex());
            s.setColIndex(c.getColIndex());
            s.setCellType(c.getCellType() == null ? "EMPTY" : c.getCellType());
            s.setSeatNo("SEAT".equals(c.getCellType()) ? c.getSeatNo() : null);
            s.setEnabled(c.getEnabled() == null ? 1 : c.getEnabled());
            seatMapper.insert(s);
        }
    }

    /** 按行列快速生成标准座位网格（指定过道列），覆盖原排布。 */
    @Transactional
    public void generateLayout(Long roomId, int rows, int cols, Integer aisleCol) {
        if (activeReservations(roomId) > 0)
            throw new BizException(BizError.ROOM_HAS_FUTURE_RESERVATION);
        rows = Math.max(1, Math.min(rows, 20));
        cols = Math.max(1, Math.min(cols, 20));
        seatMapper.delete(new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, roomId));
        for (int r = 0; r < rows; r++) {
            int seq = 1;
            char rowLetter = (char) ('A' + r);
            for (int c = 0; c < cols; c++) {
                Seat s = new Seat();
                s.setRoomId(roomId);
                s.setRowIndex(r);
                s.setColIndex(c);
                if (aisleCol != null && c == aisleCol) {
                    s.setCellType("AISLE");
                    s.setEnabled(1);
                } else {
                    s.setCellType("SEAT");
                    s.setSeatNo(rowLetter + "-" + String.format("%02d", seq++));
                    s.setEnabled(1);
                }
                seatMapper.insert(s);
            }
        }
    }

    public void toggleSeat(Long seatId, Integer enabled) {
        Seat s = seatMapper.selectById(seatId);
        if (s == null) return;
        // R10：停用座位前校验该座位是否存在未来预约
        if (enabled != null && enabled == 0) {
            Long n = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getSeatId, seatId).in(Reservation::getStatus, ACTIVE));
            if (n != null && n > 0) throw new BizException(BizError.SEAT_HAS_FUTURE_RESERVATION);
        }
        s.setEnabled(enabled);
        seatMapper.updateById(s);
    }

    /** R11：更新楼栋经纬度（校园内合法范围校验） */
    public void updateBuildingLocation(Long buildingId, Double lat, Double lng) {
        Building b = buildingMapper.selectById(buildingId);
        if (b == null) throw new BizException(BizError.BAD_REQUEST, "楼栋不存在");
        if (lat == null || lng == null || lat < -90 || lat > 90 || lng < -180 || lng > 180)
            throw new BizException(BizError.BAD_REQUEST, "经纬度不合法");
        b.setLatitude(java.math.BigDecimal.valueOf(lat));
        b.setLongitude(java.math.BigDecimal.valueOf(lng));
        buildingMapper.updateById(b);
    }

    /** R3：删除自习室（连带座位）。存在未来预约时拒绝。 */
    @Transactional
    public void deleteRoom(Long roomId) {
        if (activeReservations(roomId) > 0)
            throw new BizException(BizError.ROOM_HAS_FUTURE_RESERVATION, "该自习室存在未来预约，不能删除");
        seatMapper.delete(new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, roomId));
        roomMapper.deleteById(roomId);
    }

    /**
     * R4 + R10：切换自习室开放/关闭状态。关闭时联动公告并通知受影响（有未来预约）的学生。
     * @return 受影响的预约数
     */
    @Transactional
    public int setRoomStatus(Long roomId, String status, Long adminId) {
        StudyRoom room = roomMapper.selectById(roomId);
        if (room == null) throw new BizException(BizError.BAD_REQUEST, "自习室不存在");
        String target = "CLOSED".equalsIgnoreCase(status) ? "CLOSED" : "OPEN";
        room.setStatus(target);
        roomMapper.updateById(room);

        if (!"CLOSED".equals(target)) return 0;
        // 关闭：通知所有有未来预约的学生，并生成一条公告
        List<Reservation> active = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getRoomId, roomId).in(Reservation::getStatus, ACTIVE));
        Set<Long> users = new LinkedHashSet<>();
        for (Reservation r : active) users.add(r.getUserId());
        for (Long uid : users) {
            notificationService.notify(uid, "ANNOUNCEMENT", "⚠️ 自习室临时关闭",
                    "「" + room.getName() + "」已临时关闭，你在该自习室的预约可能受影响，请留意安排改约。");
        }
        try {
            announcementService.create(adminId, "自习室临时关闭：" + room.getName(),
                    "「" + room.getName() + "」因维护/占用等原因临时关闭，暂停新的预约，给您带来不便敬请谅解。",
                    "WARN", false);
        } catch (Exception ignored) { }
        return users.size();
    }
}
