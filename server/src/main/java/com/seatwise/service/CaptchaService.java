package com.seatwise.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图形验证码：随机 4 字符，服务端存 Redis（5 分钟），前端展示 SVG。注册时校验，防自动化注册。
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final String[] COLORS = {"#3b6cff", "#8f5bff", "#1f9d55", "#d98a00", "#d64545", "#22c1c3"};

    private final RedissonClient redisson;

    /** 生成验证码：返回 captchaId、SVG 图（data URI）、以及 code（演示便利，前端不使用） */
    public Map<String, Object> generate() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) code.append(ALPHABET[rnd.nextInt(ALPHABET.length)]);
        String id = UUID.randomUUID().toString().replace("-", "");
        redisson.getBucket(key(id)).set(code.toString(), Duration.ofMinutes(5));

        String svg = buildSvg(code.toString(), rnd);
        String dataUri = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> m = new HashMap<>();
        m.put("captchaId", id);
        m.put("image", dataUri);
        m.put("code", code.toString());   // 演示项目便利字段：正式环境应移除
        return m;
    }

    /** 校验并消费（一次性）。大小写不敏感。 */
    public boolean validate(String id, String input) {
        if (id == null || input == null) return false;
        Object v = redisson.getBucket(key(id)).getAndDelete();
        return v != null && v.toString().equalsIgnoreCase(input.trim());
    }

    private String key(String id) { return "captcha:" + id; }

    private String buildSvg(String code, ThreadLocalRandom rnd) {
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns='http://www.w3.org/2000/svg' width='120' height='42'>");
        sb.append("<rect width='120' height='42' fill='#f2f4fb' rx='6'/>");
        // 干扰线
        for (int i = 0; i < 4; i++) {
            sb.append("<line x1='").append(rnd.nextInt(120)).append("' y1='").append(rnd.nextInt(42))
              .append("' x2='").append(rnd.nextInt(120)).append("' y2='").append(rnd.nextInt(42))
              .append("' stroke='").append(COLORS[rnd.nextInt(COLORS.length)]).append("' stroke-width='1' opacity='0.5'/>");
        }
        // 字符
        for (int i = 0; i < code.length(); i++) {
            int x = 16 + i * 26 + rnd.nextInt(6) - 3;
            int y = 30 + rnd.nextInt(6) - 3;
            int rot = rnd.nextInt(40) - 20;
            sb.append("<text x='").append(x).append("' y='").append(y)
              .append("' font-size='24' font-family='monospace' font-weight='bold' fill='")
              .append(COLORS[rnd.nextInt(COLORS.length)])
              .append("' transform='rotate(").append(rot).append(' ').append(x).append(' ').append(y).append(")'>")
              .append(code.charAt(i)).append("</text>");
        }
        sb.append("</svg>");
        return sb.toString();
    }
}
