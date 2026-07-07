package com.seatwise.common;

import lombok.Data;

import java.util.UUID;

/**
 * 统一响应结构：{ code, message, data, traceId }。
 * 成功 code = "0"；失败 code = 业务错误码字符串。
 */
@Data
public class R<T> {
    private String code;
    private String message;
    private T data;
    private String traceId;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = "0";
        r.message = "ok";
        r.data = data;
        r.traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(String code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        r.traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return r;
    }
}
