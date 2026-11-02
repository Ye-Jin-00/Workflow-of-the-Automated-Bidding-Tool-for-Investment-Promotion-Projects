package com.sincit.bid.common;

import org.springframework.http.HttpStatus;

/**
 * 可预期的业务异常。HTTP 状态与业务码分离，便于前端统一处理。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_PARAM, message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.STATE_CONFLICT, message, HttpStatus.CONFLICT);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCode.UNAUTHORIZED, message, HttpStatus.UNAUTHORIZED);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
