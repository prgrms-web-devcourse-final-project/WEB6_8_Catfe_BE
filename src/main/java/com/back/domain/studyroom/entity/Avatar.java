package com.back.domain.studyroom.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 아바타 마스터 테이블
 * - 선택 가능한 모든 아바타 정보를 관리
 * - 고양이, 강아지 등 다양한 아바타로 확장 가능
 * - isDefault=true인 아바타(1,2,3)는 VISITOR 랜덤 배정용
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "avatars")
public class Avatar extends BaseEntity {
    
    @Column(nullable = false, length = 50)
    private String name;           // "검은 고양이", "하얀 고양이", "골든 리트리버" 등등등
    @Column(nullable = false, length = 500)
    private String imageUrl;       // CDN URL
    @Column(length = 200)
    private String description;    // "귀여운 검은 고양이"
    @Column(nullable = false)
    private boolean isDefault;     // 기본(랜덤) 아바타 여부
    @Column(nullable = false)
    private int sortOrder;         // 표시 순서 (1, 2, 3...)
    @Column(length = 50)
    private String category;       // "CAT", "DOG", "ETC" 등 (추후 확장 가능을 위해 카테고리를 나눴드아)
}
