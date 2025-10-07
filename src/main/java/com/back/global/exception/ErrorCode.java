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
    SOCIAL_PASSWORD_CHANGE_FORBIDDEN(HttpStatus.FORBIDDEN, "USER_010", "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다."),

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
    CANNOT_CHANGE_OWN_ROLE(HttpStatus.BAD_REQUEST, "ROOM_012", "자신의 역할은 변경할 수 없습니다."),
    CHAT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "ROOM_013", "채팅 삭제 권한이 없습니다. 방장 또는 부방장만 가능합니다."),
    INVALID_DELETE_CONFIRMATION(HttpStatus.BAD_REQUEST, "ROOM_014", "삭제 확인 메시지가 일치하지 않습니다."),
    CHAT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ROOM_015", "채팅 삭제 중 오류가 발생했습니다."),

    // ======================== 스터디 플래너 관련 ========================
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_001", "존재하지 않는 학습 계획입니다."),
    PLAN_FORBIDDEN(HttpStatus.FORBIDDEN, "PLAN_002", "학습 계획에 대한 접근 권한이 없습니다."),
    PLAN_EXCEPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_003", "학습 계획의 예외가 존재하지 않습니다."),
    PLAN_ORIGINAL_REPEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_004", "해당 날짜에 원본 반복 계획을 찾을 수 없습니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "PLAN_005", "날짜 형식이 올바르지 않습니다. (YYYY-MM-DD 형식을 사용해주세요)"),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "PLAN_006", "시작 시간은 종료 시간보다 빨라야 합니다."),
    PLAN_TIME_CONFLICT(HttpStatus.CONFLICT, "PLAN_007", "이미 존재하는 학습 계획과 시간이 겹칩니다. 기존 종료 시간과 겹치는 경우는 제외됩니다."),
    PLAN_CANNOT_UPDATE(HttpStatus.BAD_REQUEST, "PLAN_008", "수정 스위치 로직 탈출. 어떤 경우인지 파악이 필요합니다."),
    PLAN_TOO_MANY_EXCEPTIONS(HttpStatus.BAD_REQUEST, "PLAN_009", "변경사항이 비정상적으로 많아 요청이 거절되었습니다."),
    REPEAT_INVALID_UNTIL_DATE(HttpStatus.BAD_REQUEST, "REPEAT_001", "반복 계획의 종료 날짜는 시작 날짜 이전일 수 없습니다."),
    REPEAT_BYDAY_REQUIRED(HttpStatus.BAD_REQUEST, "REPEAT_002", "주간 반복 계획의 경우 요일(byDay) 정보가 필요합니다."),

    // ======================== 투두 관련 ========================
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "TODO_001", "존재하지 않는 할 일입니다."),
    TODO_FORBIDDEN(HttpStatus.FORBIDDEN, "TODO_002", "할 일에 대한 접근 권한이 없습니다."),

    // ======================== 학습 기록 관련 ========================
    DURATION_MISMATCH(HttpStatus.BAD_REQUEST, "RECORD_001", "받은 duration과 계산된 duration이 5초 이상 차이납니다."),

    // ======================== 알림 관련 ========================
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMO_001", "존재하지 않는 메모입니다."),

    // ======================== 알림 관련 ========================
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_001", "존재하지 않는 알림입니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "NOTIFICATION_002", "알림에 대한 접근 권한이 없습니다."),
    NOTIFICATION_ALREADY_READ(HttpStatus.BAD_REQUEST, "NOTIFICATION_003", "이미 읽은 알림입니다."),
    NOTIFICATION_INVALID_TARGET_TYPE(HttpStatus.BAD_REQUEST, "NOTIFICATION_004", "유효하지 않은 알림 타입입니다."),
    NOTIFICATION_MISSING_ACTOR(HttpStatus.BAD_REQUEST, "NOTIFICATION_005", "발신자 정보가 필요합니다."),
    NOTIFICATION_MISSING_TARGET(HttpStatus.BAD_REQUEST, "NOTIFICATION_006", "수신자 또는 대상 정보가 필요합니다."),

    // ======================== 메시지 관련 ========================
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSAGE_001", "존재하지 않는 메시지입니다."),
    MESSAGE_FORBIDDEN(HttpStatus.FORBIDDEN, "MESSAGE_002", "자신의 메시지만 삭제할 수 있습니다."),
    MESSAGE_NOT_IN_ROOM(HttpStatus.BAD_REQUEST, "MESSAGE_003", "해당 방의 메시지가 아닙니다."),
    MESSAGE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "MESSAGE_004", "메시지를 삭제할 권한이 없습니다."),

    // ======================== WebSocket 관련 ========================
    WS_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "WS_001", "존재하지 않는 방입니다"),
    WS_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WS_002", "WebSocket 연결에 실패했습니다."),
    WS_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "WS_003", "WebSocket 세션을 찾을 수 없습니다."),
    WS_SESSION_EXPIRED(HttpStatus.GONE, "WS_004", "WebSocket 세션이 만료되었습니다."),
    WS_REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WS_005", "세션 저장소 오류가 발생했습니다."),
    WS_ROOM_JOIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WS_006", "방 입장 처리 중 오류가 발생했습니다."),
    WS_ROOM_LEAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WS_007", "방 퇴장 처리 중 오류가 발생했습니다."),
    WS_ACTIVITY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WS_008", "활동 시간 업데이트 중 오류가 발생했습니다."),
    WS_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "WS_009", "WebSocket 인증이 필요합니다."),
    WS_FORBIDDEN(HttpStatus.FORBIDDEN, "WS_010", "WebSocket 접근 권한이 없습니다."),
    WS_INVALID_DELETE_CONFIRMATION(HttpStatus.BAD_REQUEST, "WS_011", "삭제 확인 메시지가 일치하지 않습니다."),
    WS_CHAT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WS_012", "채팅 삭제 중 오류가 발생했습니다."),
    WS_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "WS_013", "WebSocket 사용자를 찾을 수 없습니다."),
    WS_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "WS_014", "잘못된 WebSocket 요청입니다."),
    WS_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WS_015", "WebSocket 내부 오류가 발생했습니다."),
    WS_CHAT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "WS_016", "채팅 삭제 권한이 없습니다. 방장 또는 부방장만 가능합니다."),

    // ======================== 커뮤니티 관련 ========================
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_001", "존재하지 않는 게시글입니다."),
    POST_NO_PERMISSION(HttpStatus.FORBIDDEN, "POST_002", "게시글 작성자만 수정/삭제할 수 있습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_003", "존재하지 않는 카테고리입니다."),
    CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "POST_004", "이미 존재하는 카테고리입니다."),

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_001", "존재하지 않는 댓글입니다."),
    COMMENT_NO_PERMISSION(HttpStatus.FORBIDDEN, "COMMENT_002", "댓글 작성자만 수정/삭제할 수 있습니다."),
    COMMENT_PARENT_MISMATCH(HttpStatus.BAD_REQUEST, "COMMENT_003", "부모 댓글이 해당 게시글에 속하지 않습니다."),
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "COMMENT_004", "대댓글은 한 단계까지만 작성할 수 있습니다."),

    // ======================== 공통 에러 ========================
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청하신 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다."),

    // ======================== 인증/인가 에러 ========================
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 액세스 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "만료된 액세스 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005", "만료된 리프레시 토큰입니다."),
    REFRESH_TOKEN_REUSE(HttpStatus.FORBIDDEN, "AUTH_006", "재사용된 리프레시 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_007", "권한이 없습니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH_008", "지원하지 않는 소셜 로그인 제공자입니다."),
    OAUTH2_ATTRIBUTE_MISSING(HttpStatus.UNAUTHORIZED, "AUTH_009", "소셜 계정에서 필요한 사용자 정보를 가져올 수 없습니다."),
    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_010", "소셜 로그인 인증에 실패했습니다."),

    // ======================== 토큰 관련 ========================
    INVALID_EMAIL_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN_001", "유효하지 않은 이메일 인증 토큰입니다."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, "TOKEN_002", "이미 인증된 계정입니다."),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN_003", "유효하지 않은 비밀번호 재설정 토큰입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}