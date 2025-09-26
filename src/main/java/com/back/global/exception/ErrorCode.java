package com.back.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ======================== 사용자 관련 ========================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "존재하지 않는 사용자입니다."),
    USERNAME_DUPLICATED(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 아이디입니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "USER_003", "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "USER_004", "이미 사용 중인 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_005", "비밀번호는 최소 8자 이상, 숫자/특수문자를 포함해야 합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USER_006", "아이디 또는 비밀번호가 올바르지 않습니다."),
    USER_EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "USER_007", "이메일 인증 후 로그인할 수 있습니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "USER_008", "정지된 계정입니다. 관리자에게 문의하세요."),
    USER_DELETED(HttpStatus.GONE, "USER_009", "탈퇴한 계정입니다."),

    // ======================== 스터디룸 관련 ========================
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_001", "존재하지 않는 방입니다."),
    ROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "ROOM_002", "방에 대한 접근 권한이 없습니다."),
    ROOM_FULL(HttpStatus.BAD_REQUEST, "ROOM_003", "방이 가득 찼습니다."),
    ROOM_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "ROOM_004", "방 비밀번호가 틀렸습니다."),
    ROOM_INACTIVE(HttpStatus.BAD_REQUEST, "ROOM_005", "비활성화된 방입니다."),
    ROOM_TERMINATED(HttpStatus.BAD_REQUEST, "ROOM_006", "종료된 방입니다."),
    ALREADY_JOINED_ROOM(HttpStatus.BAD_REQUEST, "ROOM_007", "이미 참여 중인 방입니다."),
    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "ROOM_008", "방 멤버가 아닙니다."),
    NOT_ROOM_MANAGER(HttpStatus.FORBIDDEN, "ROOM_009", "방 관리자 권한이 필요합니다."),
    CANNOT_KICK_HOST(HttpStatus.BAD_REQUEST, "ROOM_010", "방장은 추방할 수 없습니다."),
    CANNOT_CHANGE_HOST_ROLE(HttpStatus.BAD_REQUEST, "ROOM_011", "방장의 권한은 변경할 수 없습니다."),

    // ======================== 스터디 플래너 관련 ========================
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_001", "존재하지 않는 학습 계획입니다."),
    PLAN_FORBIDDEN(HttpStatus.FORBIDDEN, "PLAN_002", "학습 계획에 대한 접근 권한이 없습니다."),
    PLAN_EXCEPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_003", "학습 계획의 예외가 존재하지 않습니다."),
    PLAN_ORIGINAL_REPEAT_NOT_FOUND(HttpStatus.BAD_REQUEST, "PLAN_004", "해당 날짜에 원본 반복 계획을 찾을 수 없습니다."),
    PLAN_INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "PLAN_005", "날짜 형식이 올바르지 않습니다. (YYYY-MM-DD 형식을 사용해주세요)"),

    // ======================== 메시지 관련 ========================
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSAGE_001", "존재하지 않는 메시지입니다."),
    MESSAGE_FORBIDDEN(HttpStatus.FORBIDDEN, "MESSAGE_002", "자신의 메시지만 삭제할 수 있습니다."),

    // ======================== WebSocket 관련 ========================
    WS_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "WS_001", "존재하지 않는 방입니다"),
    WS_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WS_002", "WebSocket 연결에 실패했습니다."),
    WS_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "WS_003", "WebSocket 세션을 찾을 수 없습니다."),
    WS_SESSION_EXPIRED(HttpStatus.GONE, "WS_004", "WebSocket 세션이 만료되었습니다."),
    WS_REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WS_005", "세션 저장소 오류가 발생했습니다."),
    WS_ROOM_JOIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WS_006", "방 입장 처리 중 오류가 발생했습니다."),
    WS_ROOM_LEAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WS_007", "방 퇴장 처리 중 오류가 발생했습니다."),
    WS_ACTIVITY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WS_008", "활동 시간 업데이트 중 오류가 발생했습니다."),

    // ======================== 공통 에러 ========================
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청하신 리소스를 찾을 수 없습니다."),

    // ======================== 인증/인가 에러 ========================
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 액세스 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "만료된 액세스 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005", "만료된 리프레시 토큰입니다."),
    REFRESH_TOKEN_REUSE(HttpStatus.FORBIDDEN, "AUTH_006", "재사용된 리프레시 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_007", "권한이 없습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}