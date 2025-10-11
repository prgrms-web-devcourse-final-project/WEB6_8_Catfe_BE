package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.Avatar;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 아바타 정보 응답 DTO
 */
@Getter
@AllArgsConstructor
public class AvatarResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private String description;
    private boolean isDefault;
    private String category;
    
    public static AvatarResponse from(Avatar avatar) {
        return new AvatarResponse(
            avatar.getId(),
            avatar.getName(),
            avatar.getImageUrl(),
            avatar.getDescription(),
            avatar.isDefault(),
            avatar.getCategory()
        );
    }
}
