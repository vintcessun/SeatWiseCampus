package com.seatwise.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class GroupReservationDTO {
    @NotNull(message = "缺少自习室")
    private Long roomId;
    @NotNull(message = "缺少日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    @NotNull(message = "缺少开始时间")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @NotNull(message = "缺少结束时间")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
    @NotEmpty(message = "缺少组队成员")
    private List<Member> members;

    @Data
    public static class Member {
        @NotNull(message = "缺少座位")
        private Long seatId;
        @NotNull(message = "缺少成员用户名")
        private String username;
    }
}
