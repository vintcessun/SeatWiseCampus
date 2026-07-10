package com.seatwise.common;

/**
 * 业务错误码，与 GLOSSARY.md / server/03-api-design.md 保持一致。
 */
public enum BizError {
    AUTH_REQUIRED(401, "请先登录"),
    PERMISSION_DENIED(403, "无权限"),
    SEAT_ALREADY_RESERVED(409, "座位已被抢占，请重新选择"),
    SEAT_ALREADY_HELD(409, "座位正被他人选择中，请稍后再试"),
    RESERVATION_TIME_CONFLICT(409, "你在该时段已有预约"),
    DAILY_LIMIT_EXCEEDED(400, "今日预约次数已达上限"),
    USER_IN_BLACKLIST(403, "你已被列入黑名单，暂不能预约"),
    SIGN_IN_TIMEOUT(400, "签到超时"),
    SIGN_IN_TOO_EARLY(400, "还未到签到时间"),
    USERNAME_EXISTS(400, "用户名已存在"),
    WAITLIST_INVALID(400, "候补状态不可操作"),
    ROOM_CLOSED(400, "自习室已关闭，暂不可预约"),
    SEAT_HAS_FUTURE_RESERVATION(409, "该座位存在未来预约，不能停用/修改"),
    ROOM_HAS_FUTURE_RESERVATION(409, "该自习室存在未来预约，不能重排座位"),
    RESERVATION_NOT_FOUND(404, "预约不存在"),
    INVALID_TIME_RANGE(400, "预约时间不合法"),
    SCORE_RULE_NOT_FOUND(400, "积分规则缺失"),
    NO_AVAILABLE_ROOM_NEARBY(404, "附近暂无可用自习室"),
    GEO_LOCATION_REQUIRED(400, "需要提供当前位置"),
    LOGIN_FAILED(400, "用户名或密码错误"),
    BAD_REQUEST(400, "请求参数错误"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int httpStatus;
    private final String defaultMessage;

    BizError(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public String code() {
        return name();
    }
}
