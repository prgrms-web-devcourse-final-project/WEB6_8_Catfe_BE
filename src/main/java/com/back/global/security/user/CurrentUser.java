package com.back.global.security.user;

import com.back.domain.user.entity.Role;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SecurityContext에 저장된 인증 정보를 바탕으로
 * 현재 로그인한 사용자 정보를 가져오는 유틸 클래스
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final UserRepository userRepository;

    /**
     * 현재 사용자가 인증된 상태인지 확인
     */
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails;
    }

    public Long getUserId() { return getDetails().getUserId(); }
    
    /**
     * 현재 사용자 ID 조회 (비로그인 시 null 반환)
     * 비로그인 접근이 허용되는 API에서 사용
     */
    public Long getUserIdOrNull() {
        try {
            return getDetails().getUserId();
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.UNAUTHORIZED) {
                return null;
            }
            throw e;
        }
    }

    public String getUsername() { return getDetails().getUsername(); }

    public Role getRole() { return getDetails().getRole(); }

    public String getEmail() { return getUserFromDb().getEmail(); }

    public String getProvider() { return getUserFromDb().getProvider(); }

    public String getProviderId() { return getUserFromDb().getProviderId(); }

    public String getStatus() { return getUserFromDb().getUserStatus().name(); }

    /**
     * SecurityContext에서 CustomUserDetails 추출
     */
    private CustomUserDetails getDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * DB에서 현재 사용자 엔티티 조회
     */
    private User getUserFromDb() {
        return userRepository.findById(getDetails().getUserId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
