package com.seatwise.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final BizError error;

    public BizException(BizError error) {
        super(error.defaultMessage());
        this.error = error;
    }

    public BizException(BizError error, String message) {
        super(message);
        this.error = error;
    }

    public static BizException of(BizError error) {
        return new BizException(error);
    }
}
