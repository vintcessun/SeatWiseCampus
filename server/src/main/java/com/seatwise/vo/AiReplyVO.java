package com.seatwise.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiReplyVO {
    private String reply;            // 面向用户的自然语言回复
    private String source;           // "llm" 或 "rule"（是否用了大模型）
    private Intent intent;           // 解析出的结构化意图
    private List<Rec> recommendations;

    @Data
    public static class Intent {
        private String date;
        private String start;
        private String end;
        private Integer durationSlots;
        private List<String> tags;
        private String buildingHint;
    }

    @Data
    public static class Rec {
        private Long roomId;
        private String roomName;
        private String buildingName;
        private Long seatId;
        private String seatNo;
        private String start;
        private String end;
        private Double score;
        private List<String> reasons;
        private List<String> tags;
        private Integer availableSeats;
    }
}
