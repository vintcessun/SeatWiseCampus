package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.seatwise.common.R;
import com.seatwise.service.ReservationService;
import com.seatwise.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationService reservationService;

    /** 按学生姓名/学号 + 状态/日期 追踪预约记录 */
    @SaCheckRole("ADMIN")
    @GetMapping("/reservations")
    public R<List<ReservationVO>> search(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(reservationService.adminSearch(keyword, status, date));
    }
}
