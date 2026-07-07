package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.dto.LayoutDTO;
import com.seatwise.entity.*;
import com.seatwise.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BaseDataService {

    private final CampusMapper campusMapper;
    private final BuildingMapper buildingMapper;
    private final StudyRoomMapper roomMapper;
    private final SeatMapper seatMapper;

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

    @Transactional
    public void saveLayout(Long roomId, LayoutDTO dto) {
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

    public void toggleSeat(Long seatId, Integer enabled) {
        Seat s = seatMapper.selectById(seatId);
        if (s != null) {
            s.setEnabled(enabled);
            seatMapper.updateById(s);
        }
    }
}
