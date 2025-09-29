package com.back.global.security.oauth;

import java.util.Map;

/**
 * 깃허브 OAuth2 사용자 정보 구현체
 */
public class GithubOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "github";
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id")); // GitHub user id
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email"); // 이메일 공개 설정 안 돼 있으면 null
    }

    @Override
    public String getNickname() {
        return (String) attributes.get("login"); // GitHub username
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("avatar_url");
    }
}
