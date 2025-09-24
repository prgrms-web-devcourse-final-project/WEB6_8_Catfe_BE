package com.back.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ======================== 사용자 관련 ========================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "존재하지 않는 사용자입니다."),
    USERNAME_DUPLICATED(HttpStatus.CONFLICT, "USER_409", "이미 사용 중인 아이디입니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "USER_409", "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "USER_409", "이미 사용 중인 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_400", "비밀번호는 최소 8자 이상, 숫자/특수문자를 포함해야 합니다."),

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

    // ======================== 메시지 관련 ========================
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSAGE_001", "존재하지 않는 메시지입니다."),
    MESSAGE_FORBIDDEN(HttpStatus.FORBIDDEN, "MESSAGE_002", "자신의 메시지만 삭제할 수 있습니다."),

    // ======================== WebSocket 관련 ========================
    WS_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "WS_001", "존재하지 않는 방입니다"),

    // ======================== 공통 에러 ========================
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청하신 리소스를 찾을 수 없습니다."),

    // ======================== 인증/인가 에러 ========================
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401", "유효하지 않은 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401", "만료된 액세스 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401", "만료된 리프레시 토큰입니다."),
    REFRESH_TOKEN_REUSE(HttpStatus.FORBIDDEN, "AUTH_403", "재사용된 리프레시 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_403", "권한이 없습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}