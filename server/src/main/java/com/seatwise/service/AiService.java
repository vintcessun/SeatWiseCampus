package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatwise.common.SeatTags;
import com.seatwise.common.SlotUtil;
import com.seatwise.config.AiProps;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.entity.Building;
import com.seatwise.entity.Seat;
import com.seatwise.entity.StudyRoom;
import com.seatwise.mapper.BuildingMapper;
import com.seatwise.mapper.SeatMapper;
import com.seatwise.vo.AiReplyVO;
import com.seatwise.vo.BoardVO;
import com.seatwise.vo.SeatStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * AI 智能选座助手：自然语言 → 意图（OpenAI 兼容 LLM，失败/未配置时降级关键词解析）→ 可解释座位推荐。
 * 推荐正确性由确定性引擎保证，LLM 只负责理解与措辞，不臆造可用性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProps aiProps;
    private final BaseDataService baseDataService;
    private final BoardService boardService;
    private final SeatMapper seatMapper;
    private final BuildingMapper buildingMapper;
    private final SeatwiseProps props;

    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6)).build();

    public AiReplyVO assistant(String message, Long campusId, String dateStr) {
        int slotMin = props.getSlotMinutes();
        LocalDate today = LocalDate.now();
        LocalDate date = dateStr != null && !dateStr.isBlank() ? LocalDate.parse(dateStr) : today;

        // 1) 解析意图
        AiReplyVO.Intent intent = new AiReplyVO.Intent();
        String source = "rule";
        boolean llmOk = false;
        if (aiProps.llmEnabled()) {
            try {
                fillIntentByLlm(message, date, intent);
                llmOk = true;
                source = "llm";
            } catch (Exception e) {
                log.warn("LLM 解析失败，降级规则引擎: {}", e.getMessage());
            }
        }
        if (!llmOk) {
            fillIntentByRule(message, date, intent);
        }

        // 2) 规范化时间窗
        int slotMinutes = slotMin;
        LocalTime start = parseTime(intent.getStart(), LocalTime.of(14, 0));
        int durationSlots = intent.getDurationSlots() != null && intent.getDurationSlots() > 0
                ? Math.min(intent.getDurationSlots(), props.getMaxSlotsPerReservation()) : 4;
        int startSlot = SlotUtil.toSlot(start, slotMinutes);
        int endSlot = startSlot + durationSlots;
        LocalTime end = SlotUtil.slotToTime(endSlot, slotMinutes);
        LocalDate intentDate = intent.getDate() != null && !intent.getDate().isBlank()
                ? LocalDate.parse(intent.getDate()) : date;
        intent.setStart(SlotUtil.label(startSlot, slotMinutes));
        intent.setEnd(SlotUtil.label(endSlot, slotMinutes));
        intent.setDate(intentDate.toString());
        if (intent.getTags() == null) intent.setTags(List.of());

        // 3) 可解释推荐
        List<AiReplyVO.Rec> recs = recommend(intentDate, startSlot, endSlot,
                intent.getTags(), intent.getBuildingHint(), campusId == null ? 1L : campusId);

        AiReplyVO vo = new AiReplyVO();
        vo.setIntent(intent);
        vo.setRecommendations(recs);
        vo.setSource(source);
        if (recs.isEmpty()) {
            vo.setReply("抱歉，在 " + intent.getStart() + "-" + intent.getEnd()
                    + " 没有找到满足条件的空闲座位，换个时间段或放宽偏好试试～");
        } else {
            AiReplyVO.Rec top = recs.get(0);
            String tagText = top.getTags().isEmpty() ? "" :
                    "（" + String.join("、", top.getTags().stream().map(SeatTags::cn).toList()) + "）";
            vo.setReply("为你推荐「" + top.getRoomName() + "」的 " + top.getSeatNo() + " 号座位"
                    + tagText + "：从 " + top.getStart() + " 起连续空闲 "
                    + (durationSlots * slotMinutes) + " 分钟，" + reasonTail(top) + "。点击卡片即可前往预约。");
        }
        return vo;
    }

    private String reasonTail(AiReplyVO.Rec r) {
        // 跳过第一条（连续空闲时长，已在前文说明），只拼后续理由
        if (r.getReasons().size() <= 1) return "位置合适";
        return String.join("，", r.getReasons().subList(1, r.getReasons().size()));
    }

    // ===================== 推荐引擎 =====================
    private List<AiReplyVO.Rec> recommend(LocalDate date, int startSlot, int endSlot,
                                          List<String> desiredTags, String buildingHint, Long campusId) {
        List<StudyRoom> rooms = baseDataService.listRooms(campusId, null, null);
        List<AiReplyVO.Rec> all = new ArrayList<>();
        for (StudyRoom room : rooms) {
            if (room.getStatus() != null && !"OPEN".equalsIgnoreCase(room.getStatus())) continue;
            Building building = buildingMapper.selectById(room.getBuildingId());
            boolean buildingMatch = buildingHint != null && !buildingHint.isBlank() && (
                    (room.getName() != null && room.getName().contains(buildingHint)) ||
                    (building != null && building.getName() != null && building.getName().contains(buildingHint)));

            List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, room.getId()));
            int cols = seats.stream().map(Seat::getColIndex).filter(Objects::nonNull).mapToInt(i -> i + 1).max().orElse(8);
            Map<Long, Seat> seatMap = new HashMap<>();
            for (Seat s : seats) seatMap.put(s.getId(), s);

            BoardVO board = boardService.buildBoard(room.getId(), date, startSlot, endSlot, null);
            List<SeatStatusVO> free = board.getSeats().stream().filter(s -> "FREE".equals(s.getStatus())).toList();
            long totalSeat = seats.stream().filter(s -> "SEAT".equals(s.getCellType())).count();
            double availRatio = totalSeat == 0 ? 0 : (double) free.size() / totalSeat;

            for (SeatStatusVO s : free) {
                Seat seat = seatMap.get(s.getSeatId());
                if (seat == null) continue;
                List<String> tags = (seat.getTags() != null && !seat.getTags().isBlank())
                        ? SeatTags.parse(seat.getTags())
                        : SeatTags.of(seat, room, cols);
                double tagScore = desiredTags == null || desiredTags.isEmpty() ? 0.5
                        : (double) desiredTags.stream().filter(tags::contains).count() / desiredTags.size();
                double score = 0.30 * 1.0                       // 连续可用（FREE 已保证整段空闲）
                        + 0.30 * tagScore
                        + 0.20 * (buildingMatch ? 1.0 : 0.0)
                        + 0.20 * availRatio;

                AiReplyVO.Rec rec = new AiReplyVO.Rec();
                rec.setRoomId(room.getId());
                rec.setRoomName(room.getName());
                rec.setBuildingName(building != null ? building.getName() : "");
                rec.setSeatId(seat.getId());
                rec.setSeatNo(seat.getSeatNo());
                rec.setStart(SlotUtil.label(startSlot, props.getSlotMinutes()));
                rec.setEnd(SlotUtil.label(endSlot, props.getSlotMinutes()));
                rec.setScore(Math.round(score * 100) / 100.0);
                rec.setTags(tags);
                rec.setAvailableSeats(free.size());
                List<String> reasons = new ArrayList<>();
                reasons.add("连续空闲 " + ((endSlot - startSlot) * props.getSlotMinutes()) + " 分钟");
                if (desiredTags != null) {
                    for (String t : desiredTags) if (tags.contains(t)) reasons.add(SeatTags.cn(t));
                }
                if (buildingMatch) reasons.add("在你想去的" + buildingHint);
                reasons.add("周边空位率 " + Math.round(availRatio * 100) + "%");
                rec.setReasons(reasons);
                all.add(rec);
            }
        }
        all.sort(Comparator.comparingDouble(AiReplyVO.Rec::getScore).reversed());
        // 每个房间最多 1 个，取前 3
        List<AiReplyVO.Rec> result = new ArrayList<>();
        Set<Long> usedRooms = new HashSet<>();
        for (AiReplyVO.Rec r : all) {
            if (usedRooms.add(r.getRoomId())) result.add(r);
            if (result.size() >= 3) break;
        }
        return result;
    }

    // ===================== LLM 意图解析（OpenAI 兼容） =====================
    private void fillIntentByLlm(String message, LocalDate today, AiReplyVO.Intent intent) throws Exception {
        String sys = "你是校园自习室预约助手的意图解析器。只输出 JSON，不要解释。字段：" +
                "date(yyyy-MM-dd，缺省用今天 " + today + ")、start(HH:mm，缺省 14:00)、durationSlots(整数，30分钟为1，缺省4)、" +
                "tags(数组，取值仅限 window/power/quiet/discuss)、buildingHint(楼栋关键词或null)。" +
                "示例输出：{\"date\":\"" + today + "\",\"start\":\"14:00\",\"durationSlots\":4,\"tags\":[\"quiet\",\"window\"],\"buildingHint\":\"图书馆\"}";

        ObjectNode body = om.createObjectNode();
        body.put("model", aiProps.getModel());
        body.put("temperature", 0.1);
        ArrayNode msgs = body.putArray("messages");
        ObjectNode m1 = msgs.addObject(); m1.put("role", "system"); m1.put("content", sys);
        ObjectNode m2 = msgs.addObject(); m2.put("role", "user"); m2.put("content", message);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(aiProps.getBaseUrl().replaceAll("/+$", "") + "/chat/completions"))
                .timeout(Duration.ofMillis(aiProps.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiProps.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) throw new RuntimeException("LLM HTTP " + resp.statusCode());
        JsonNode root = om.readTree(resp.body());
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        content = content.replaceAll("```json", "").replaceAll("```", "").trim();
        JsonNode j = om.readTree(content);
        if (j.hasNonNull("date")) intent.setDate(j.get("date").asText());
        if (j.hasNonNull("start")) intent.setStart(j.get("start").asText());
        if (j.hasNonNull("durationSlots")) intent.setDurationSlots(j.get("durationSlots").asInt());
        if (j.hasNonNull("buildingHint")) intent.setBuildingHint(j.get("buildingHint").asText());
        List<String> tags = new ArrayList<>();
        if (j.has("tags") && j.get("tags").isArray()) j.get("tags").forEach(t -> tags.add(t.asText()));
        intent.setTags(tags);
    }

    // ===================== 规则解析（离线兜底） =====================
    private void fillIntentByRule(String msg, LocalDate today, AiReplyVO.Intent intent) {
        String m = msg == null ? "" : msg;
        intent.setDate(today.toString());
        // 时间：匹配 "14:00" 或 "14点" 或 下午/上午/晚上
        java.util.regex.Matcher hm = java.util.regex.Pattern.compile("(\\d{1,2})[:：点](\\d{0,2})").matcher(m);
        if (hm.find()) {
            int h = Integer.parseInt(hm.group(1));
            int mi = hm.group(2).isEmpty() ? 0 : Integer.parseInt(hm.group(2));
            if (m.contains("下午") && h < 12) h += 12;
            if (m.contains("晚上") && h < 18) h += 12;
            intent.setStart(String.format("%02d:%02d", h, mi >= 30 ? 30 : 0));
        } else if (m.contains("下午")) intent.setStart("14:00");
        else if (m.contains("上午")) intent.setStart("10:00");
        else if (m.contains("晚上")) intent.setStart("19:00");
        else intent.setStart("14:00");

        // 时长
        java.util.regex.Matcher dm = java.util.regex.Pattern.compile("(\\d+)\\s*(小时|个小时|h)").matcher(m);
        if (dm.find()) intent.setDurationSlots(Math.max(1, Integer.parseInt(dm.group(1)) * 2));
        else intent.setDurationSlots(4);

        List<String> tags = new ArrayList<>();
        if (m.contains("窗") || m.contains("靠窗")) tags.add(SeatTags.WINDOW);
        if (m.contains("插座") || m.contains("充电") || m.contains("电源")) tags.add(SeatTags.POWER);
        if (m.contains("安静") || m.contains("静音") || m.contains("考研")) tags.add(SeatTags.QUIET);
        if (m.contains("讨论") || m.contains("组队") || m.contains("小组")) tags.add(SeatTags.DISCUSS);
        intent.setTags(tags);

        if (m.contains("图书馆")) intent.setBuildingHint("图书馆");
        else if (m.contains("教学楼") || m.contains("教") ) intent.setBuildingHint("教学楼");
    }

    private LocalTime parseTime(String s, LocalTime def) {
        try { return s == null ? def : LocalTime.parse(s); } catch (Exception e) { return def; }
    }
}
