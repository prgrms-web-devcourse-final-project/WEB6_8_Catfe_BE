package com.back.domain.studyroom.service;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomInviteCode;
import com.back.domain.studyroom.repository.RoomInviteCodeRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 초대 코드 서비스
 * - DB: 영속적 저장 (생성 이력, 만료 관리)
 * - Redis: 빠른 검증, TTL 자동 만료
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomInviteService {

    private final RoomInviteCodeRepository inviteCodeRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 혼동 문자 제외
    private static final int CODE_LENGTH = 8;
    private static final Random RANDOM = new SecureRandom();
    private static final String REDIS_KEY_PREFIX = "invite:code:";
    private static final int EXPIRY_HOURS = 3;

    /**
     * 내 초대 코드 조회 (없으면 생성)
     * - 활성 코드가 있으면 반환
     * - 없으면 새로 생성
     * - 만료된 코드는 비활성화 표시
     */
    @Transactional
    public RoomInviteCode getOrCreateMyInviteCode(Long roomId, Long userId) {
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // 1. 사용자의 활성 초대 코드 조회 (DB)
        Optional<RoomInviteCode> existingCode = 
                inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(roomId, userId);
        
        if (existingCode.isPresent()) {
            RoomInviteCode code = existingCode.get();
            
            // 2. 만료 확인
            if (code.isValid()) {
                log.info("기존 초대 코드 반환 - RoomId: {}, UserId: {}, Code: {}", 
                        roomId, userId, code.getInviteCode());
                
                // Redis에도 저장 (없을 수 있으므로)
                saveToRedis(code);
                return code;
            } else {
                // 만료된 코드 비활성화
                code.deactivate();
                log.info("만료된 초대 코드 비활성화 - RoomId: {}, UserId: {}, Code: {}", 
                        roomId, userId, code.getInviteCode());
            }
        }
        
        // 3. 새 초대 코드 생성
        String inviteCode = generateUniqueInviteCode();
        RoomInviteCode newCode = RoomInviteCode.create(inviteCode, room, user);
        inviteCodeRepository.save(newCode);
        
        // Redis에 저장 (3시간 TTL)
        saveToRedis(newCode);
        
        log.info("새 초대 코드 생성 - RoomId: {}, UserId: {}, Code: {}, ExpiresAt: {}", 
                roomId, userId, inviteCode, newCode.getExpiresAt());
        
        return newCode;
    }

    /**
     * 초대 코드로 방 조회 및 검증
     * - Redis 우선 조회 (빠름)
     * - Redis에 없으면 DB 조회
     */
    public Room getRoomByInviteCode(String inviteCode) {
        
        // 1. Redis에서 먼저 확인 (빠른 조회)
        RoomInviteCode code = getFromRedis(inviteCode);
        
        // 2. Redis에 없으면 DB 조회
        if (code == null) {
            code = inviteCodeRepository.findByInviteCode(inviteCode)
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INVITE_CODE));
            
            // 유효한 코드라면 Redis에 다시 저장
            if (code.isValid()) {
                saveToRedis(code);
            }
        }
        
        // 3. 유효성 검증
        if (!code.isValid()) {
            throw new CustomException(ErrorCode.INVITE_CODE_EXPIRED);
        }
        
        Room room = code.getRoom();
        
        log.info("초대 코드 검증 완료 - Code: {}, RoomId: {}", 
                inviteCode, room.getId());
        
        return room;
    }

    /**
     * 고유한 초대 코드 생성
     */
    private String generateUniqueInviteCode() {
        int maxAttempts = 10;
        
        for (int i = 0; i < maxAttempts; i++) {
            String code = RANDOM.ints(CODE_LENGTH, 0, CODE_CHARS.length())
                    .mapToObj(idx -> String.valueOf(CODE_CHARS.charAt(idx)))
                    .collect(Collectors.joining());
            
            // DB와 Redis 모두 확인
            if (!inviteCodeRepository.existsByInviteCode(code) && 
                !existsInRedis(code)) {
                return code;
            }
        }
        
        throw new CustomException(ErrorCode.INVITE_CODE_GENERATION_FAILED);
    }

    /**
     * Redis에 저장 (3시간 TTL)
     */
    private void saveToRedis(RoomInviteCode code) {
        try {
            String key = REDIS_KEY_PREFIX + code.getInviteCode();
            
            // JSON으로 변환 (roomId만 저장)
            String value = String.valueOf(code.getRoom().getId());
            
            // 만료 시간까지의 남은 시간 계산
            long ttl = Duration.between(LocalDateTime.now(), code.getExpiresAt()).getSeconds();
            
            if (ttl > 0) {
                redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
                log.debug("Redis 저장 완료 - Code: {}, RoomId: {}, TTL: {}초", 
                        code.getInviteCode(), code.getRoom().getId(), ttl);
            }
        } catch (Exception e) {
            // Redis 저장 실패는 무시 (DB에는 저장됨)
            log.warn("Redis 저장 실패 (무시) - Code: {}", code.getInviteCode(), e);
        }
    }

    /**
     * Redis에서 조회
     */
    private RoomInviteCode getFromRedis(String inviteCode) {
        try {
            String key = REDIS_KEY_PREFIX + inviteCode;
            String value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                Long roomId = Long.parseLong(value);
                
                // DB에서 전체 정보 조회
                return inviteCodeRepository.findByInviteCode(inviteCode).orElse(null);
            }
        } catch (Exception e) {
            log.warn("Redis 조회 실패 (무시) - Code: {}", inviteCode, e);
        }
        return null;
    }

    /**
     * Redis에 존재 여부 확인
     */
    private boolean existsInRedis(String inviteCode) {
        try {
            String key = REDIS_KEY_PREFIX + inviteCode;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis 존재 확인 실패 (무시) - Code: {}", inviteCode, e);
            return false;
        }
    }
}
