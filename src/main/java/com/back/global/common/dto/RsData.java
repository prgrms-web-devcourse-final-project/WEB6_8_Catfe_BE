package com.back.global.common.dto;

import com.back.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RsData<T> {
    private boolean isSuccess; // 응답 성공 여부
    private String code; // 상태 코드
    private String message; // 메시지
    private T data; // 데이터

    // 성공 응답 (응답 메시지 + 데이터 반환)
    public static <T> RsData<T> success(String message, T data) {
        return new RsData<>(true,"SUCCESS_200", message, data);
    }

    // 성공 응답 (응답 메시지 + 데이터 반환 x)
    public static <T> RsData<T> success(String message) {
        return new RsData<>(true,"SUCCESS_200", message, null);
    }

    // 실패 응답 (응답 메시지 + 데이터 반환)
    public static <T> RsData<T> fail(ErrorCode errorCode, T data) {
        return new RsData<>(false, errorCode.getCode(), errorCode.getMessage(), data);
    }

    // 실패 응답 (응답 메시지 + 데이터 반환 x)
    public static <T> RsData<T> fail(ErrorCode errorCode) {
        return new RsData<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }
}
