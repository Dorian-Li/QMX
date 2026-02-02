package com.example.qmx.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "成功"),
    PARAMS_ERROR(40001, "参数错误"),
    SYSTEM_ERROR(50000, "系统错误"),
    NOT_FOUND(404, "数据不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
