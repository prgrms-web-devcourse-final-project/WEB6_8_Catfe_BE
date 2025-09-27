package com.back.global.security.oauth;

/**
 * OAuth2UserInfo 인터페이스
 * - OAuth2 공급자(예: Kakao, Google 등)로부터 제공되는 사용자 정보를 추상화하는 인터페이스
 * - 각 공급자별로 구현체를 만들어 사용자 정보를 일관된 방식으로 접근할 수 있도록 함
 */
public interface OAuth2UserInfo {
    String getProvider();
    String getProviderId();
    String getEmail();
    String getNickname();
    String getProfileImageUrl();
}
