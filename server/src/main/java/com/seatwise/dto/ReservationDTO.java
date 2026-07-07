package com.seatwise.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservationDTO {
    @NotNull(message = "缺少自习室")
    private Long roomId;
    @NotNull(message = "缺少座位")
    private Long seatId;
    @NotNull(message = "缺少日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    @NotNull(message = "缺少开始时间")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @NotNull(message = "缺少结束时间")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
}
