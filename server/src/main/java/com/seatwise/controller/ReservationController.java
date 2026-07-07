package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.seatwise.common.R;
import com.seatwise.dto.ReservationDTO;
import com.seatwise.service.ReservationService;
import com.seatwise.vo.ReservationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    private Long uid() {
        return Long.valueOf(StpUtil.getLoginId().toString());
    }

    @SaCheckRole("STUDENT")
    @PostMapping
    public R<ReservationVO> create(@Valid @RequestBody ReservationDTO dto) {
        return R.ok(reservationService.create(uid(), dto.getRoomId(), dto.getSeatId(),
                dto.getDate(), dto.getStartTime(), dto.getEndTime()));
    }

    @SaCheckLogin
    @GetMapping("/me")
    public R<List<ReservationVO>> me() {
        return R.ok(reservationService.myReservations(uid()));
    }

    @SaCheckRole("STUDENT")
    @PostMapping("/{id}/check-in")
    public R<ReservationVO> checkIn(@PathVariable Long id) {
        return R.ok(reservationService.checkIn(uid(), id));
    }

    @SaCheckRole("STUDENT")
    @PostMapping("/{id}/check-out")
    public R<ReservationVO> checkOut(@PathVariable Long id) {
        return R.ok(reservationService.checkOut(uid(), id));
    }

    @SaCheckRole("STUDENT")
    @PostMapping("/{id}/cancel")
    public R<ReservationVO> cancel(@PathVariable Long id) {
        return R.ok(reservationService.cancel(uid(), id));
    }
}
