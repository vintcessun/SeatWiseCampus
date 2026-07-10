package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.seatwise.common.R;
import com.seatwise.dto.LayoutDTO;
import com.seatwise.entity.Building;
import com.seatwise.entity.Campus;
import com.seatwise.entity.StudyRoom;
import com.seatwise.mapper.BuildingMapper;
import com.seatwise.mapper.CampusMapper;
import com.seatwise.mapper.StudyRoomMapper;
import com.seatwise.service.BaseDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BaseDataController {

    private final BaseDataService baseDataService;
    private final CampusMapper campusMapper;
    private final BuildingMapper buildingMapper;
    private final StudyRoomMapper roomMapper;

    @GetMapping("/campuses")
    public R<List<Campus>> campuses() {
        return R.ok(baseDataService.listCampuses());
    }

    @GetMapping("/buildings")
    public R<List<Building>> buildings(@RequestParam(required = false) Long campusId) {
        return R.ok(baseDataService.listBuildings(campusId));
    }

    @GetMapping("/study-rooms")
    public R<List<StudyRoom>> rooms(@RequestParam(required = false) Long campusId,
                                    @RequestParam(required = false) Long buildingId,
                                    @RequestParam(required = false) Integer floorNo) {
        return R.ok(baseDataService.listRooms(campusId, buildingId, floorNo));
    }

    @GetMapping("/study-rooms/{id}/layout")
    public R<Map<String, Object>> getLayout(@PathVariable Long id) {
        return R.ok(baseDataService.getLayout(id));
    }

    // ================= 管理端 =================
    @SaCheckRole("ADMIN")
    @PutMapping("/study-rooms/{id}/layout")
    public R<Void> saveLayout(@PathVariable Long id, @RequestBody LayoutDTO dto) {
        baseDataService.saveLayout(id, dto);
        return R.ok();
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/seats/{seatId}/toggle")
    public R<Void> toggleSeat(@PathVariable Long seatId, @RequestParam Integer enabled) {
        baseDataService.toggleSeat(seatId, enabled);
        return R.ok();
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/study-rooms/{id}")
    public R<Void> deleteRoom(@PathVariable Long id) {
        baseDataService.deleteRoom(id);
        return R.ok();
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/study-rooms/{id}/status")
    public R<Map<String, Object>> setRoomStatus(@PathVariable Long id, @RequestParam String status) {
        Long adminId = Long.valueOf(StpUtil.getLoginId().toString());
        int affected = baseDataService.setRoomStatus(id, status, adminId);
        return R.ok(Map.of("status", status.toUpperCase(), "affected", affected));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/study-rooms/{id}/generate-layout")
    public R<Void> generateLayout(@PathVariable Long id,
                                  @RequestParam int rows,
                                  @RequestParam int cols,
                                  @RequestParam(required = false) Integer aisleCol) {
        baseDataService.generateLayout(id, rows, cols, aisleCol);
        return R.ok();
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/campuses")
    public R<Campus> createCampus(@RequestBody Campus c) {
        campusMapper.insert(c);
        return R.ok(c);
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/buildings")
    public R<Building> createBuilding(@RequestBody Building b) {
        buildingMapper.insert(b);
        return R.ok(b);
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/study-rooms")
    public R<StudyRoom> createRoom(@RequestBody StudyRoom room) {
        if (room.getStatus() == null) room.setStatus("OPEN");
        roomMapper.insert(room);
        return R.ok(room);
    }
}
