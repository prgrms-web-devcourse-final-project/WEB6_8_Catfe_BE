package com.back.global.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security에서 사용하는 사용자 인증 정보 클래스
 * - JWT에서 파싱한 사용자 정보를 담고 있음
 */
@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {
    private Long userId;
    private String username;
    private String role;

    @Override
    public Collection<SimpleGrantedAuthority> getAuthorities() {
        // Spring Security 권한 체크는 "ROLE_" prefix 필요
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        // JWT 인증에서는 비밀번호를 사용하지 않음
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}