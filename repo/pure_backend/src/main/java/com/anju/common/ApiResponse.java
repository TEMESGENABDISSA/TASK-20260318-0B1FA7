package com.anju.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "OK", data);
    }
}
