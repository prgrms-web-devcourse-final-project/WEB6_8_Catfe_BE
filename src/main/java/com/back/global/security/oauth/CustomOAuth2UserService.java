package com.back.global.security.oauth;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.repository.UserProfileRepository;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 소셜 로그인(OAuth2) 사용자의 정보를 받아 DB에 매핑하는 서비스 클래스
 *
 * 주요 기능:
 * 1. 소셜 로그인 제공자(카카오 등)에서 사용자 정보를 받아옴
 * 2. provider + providerId로 사용자 DB 조회
 * 3. 기존 사용자가 없으면 신규 가입 처리
 * 4. 최종적으로 SecurityContext에 저장될 CustomUserDetails를 반환
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            System.out.println("OAuth2 login: provider = " + userRequest.getClientRegistration().getRegistrationId());

            // 소셜 제공자에서 사용자 정보 로드
            OAuth2User oAuth2User = super.loadUser(userRequest);
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            // 소셜 제공자별로 사용자 정보 매핑
            OAuth2UserInfo userInfo = switch (registrationId) {
                case "kakao" -> new KakaoOAuth2UserInfo(attributes);
                default -> throw new CustomException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
            };

            // 필수 정보 검증
            if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
                throw new CustomException(ErrorCode.OAUTH2_EMAIL_NOT_FOUND);
            }
            if (userInfo.getProviderId() == null) {
                throw new CustomException(ErrorCode.OAUTH2_ATTRIBUTE_MISSING);
            }

            // DB에서 사용자 조회 또는 신규 가입 처리
            User user = userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                    .orElseGet(() -> {
                        User newUser = User.createOAuth2User(
                                userInfo.getProvider() + "_" + userInfo.getProviderId(),
                                userInfo.getEmail(),
                                userInfo.getProvider(),
                                userInfo.getProviderId()
                        );

                        UserProfile userProfile = new UserProfile(
                                newUser,
                                userInfo.getNickname(),
                                userInfo.getProfileImageUrl(),
                                null,
                                null,
                                0
                        );
                        newUser.setUserProfile(userProfile);

                        return userRepository.save(newUser);
                    });

            // SecurityContext에 저장될 사용자 객체 반환
            return new CustomUserDetails(
                    user.getId(),
                    user.getUsername(),
                    user.getRole(),
                    attributes
            );
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.OAUTH2_AUTHENTICATION_FAILED);
        }
    }
}
