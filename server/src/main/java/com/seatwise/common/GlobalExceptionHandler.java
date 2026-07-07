package com.seatwise.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        BizError err = e.getError();
        return ResponseEntity.status(err.httpStatus())
                .body(R.fail(err.code(), e.getMessage()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> handleNotLogin(NotLoginException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.fail(BizError.AUTH_REQUIRED.code(), BizError.AUTH_REQUIRED.defaultMessage()));
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<R<Void>> handleNoPermission(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.fail(BizError.PERMISSION_DENIED.code(), BizError.PERMISSION_DENIED.defaultMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : BizError.BAD_REQUEST.defaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(BizError.BAD_REQUEST.code(), msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(BizError.INTERNAL_ERROR.code(), BizError.INTERNAL_ERROR.defaultMessage()));
    }
}
