package com.back.domain.user.common.util;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;

/**
 * 비밀번호 유효성 검증 유틸리티 클래스
 *
 * 비밀번호 정책:
 * - 최소 8자 이상
 * - 최소 하나의 숫자 포함
 * - 최소 하나의 특수문자 포함 (!@#$%^&*)
 */
public class PasswordValidator {
    private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$";

    /**
     * 비밀번호 유효성 검증 메서드
     *
     * @param password 검증할 비밀번호
     * @throws CustomException 비밀번호가 정책에 맞지 않을 경우 USER_005 예외 발생
     */
    public static void validate(String password) {
        if (!password.matches(PASSWORD_REGEX)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
