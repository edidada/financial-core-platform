package com.fcp.common.api;

public record ApiResult<T>(int code, String message, T data) {
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "OK", data);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
}