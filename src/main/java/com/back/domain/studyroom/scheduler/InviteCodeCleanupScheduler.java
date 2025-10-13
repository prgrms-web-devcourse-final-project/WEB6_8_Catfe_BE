package com.back.domain.studyroom.scheduler;

import com.back.domain.studyroom.repository.RoomInviteCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 초대 코드 정리 스케줄러
 * - 만료된 초대 코드 자동 비활성화
 * - 매시간 실행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InviteCodeCleanupScheduler {

    private final RoomInviteCodeRepository inviteCodeRepository;

    /**
     * 만료된 초대 코드 정리
     * - 매시간 정각에 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredInviteCodes() {
        LocalDateTime now = LocalDateTime.now();
        
        int deactivatedCount = inviteCodeRepository.deactivateExpiredCodes(now);
        
        if (deactivatedCount > 0) {
            log.info("만료된 초대 코드 정리 완료 - 비활성화된 코드 수: {}", deactivatedCount);
        }
    }
}
