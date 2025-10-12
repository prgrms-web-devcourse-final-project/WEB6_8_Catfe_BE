package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.Avatar;
import com.back.global.config.QueryDslTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(QueryDslTestConfig.class)
@DisplayName("AvatarRepository 테스트")
class AvatarRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private AvatarRepository avatarRepository;

    private Avatar avatar1;
    private Avatar avatar2;
    private Avatar avatar3;
    private Avatar avatar4;

    @BeforeEach
    void setUp() {
        // 기본 아바타 (랜덤 배정용)
        avatar1 = Avatar.builder()
                .name("검은 고양이")
                .imageUrl("/images/avatars/cat-black.png")
                .description("귀여운 검은 고양이")
                .isDefault(true)
                .sortOrder(1)
                .category("CAT")
                .build();
        testEntityManager.persist(avatar1);

        avatar2 = Avatar.builder()
                .name("하얀 고양이")
                .imageUrl("/images/avatars/cat-white.png")
                .description("우아한 하얀 고양이")
                .isDefault(true)
                .sortOrder(2)
                .category("CAT")
                .build();
        testEntityManager.persist(avatar2);

        avatar3 = Avatar.builder()
                .name("노란 고양이")
                .imageUrl("/images/avatars/cat-orange.png")
                .description("발랄한 노란 고양이")
                .isDefault(true)
                .sortOrder(3)
                .category("CAT")
                .build();
        testEntityManager.persist(avatar3);

        // 특별 아바타 (구매 필요 등)
        avatar4 = Avatar.builder()
                .name("골든 리트리버")
                .imageUrl("/images/avatars/dog-golden.png")
                .description("친근한 골든 리트리버")
                .isDefault(false)  // 기본 아바타 아님
                .sortOrder(4)
                .category("DOG")
                .build();
        testEntityManager.persist(avatar4);

        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("정렬 순서대로 모든 아바타 조회")
    void findAllByOrderBySortOrderAsc() {
        // when
        List<Avatar> avatars = avatarRepository.findAllByOrderBySortOrderAsc();

        // then
        assertThat(avatars).hasSize(4);
        assertThat(avatars.get(0).getName()).isEqualTo("검은 고양이");
        assertThat(avatars.get(1).getName()).isEqualTo("하얀 고양이");
        assertThat(avatars.get(2).getName()).isEqualTo("노란 고양이");
        assertThat(avatars.get(3).getName()).isEqualTo("골든 리트리버");
    }

    @Test
    @DisplayName("기본 아바타만 조회 (랜덤 배정용)")
    void findByIsDefaultTrueOrderBySortOrderAsc() {
        // when
        List<Avatar> defaultAvatars = avatarRepository.findByIsDefaultTrueOrderBySortOrderAsc();

        // then
        assertThat(defaultAvatars).hasSize(3);
        assertThat(defaultAvatars)
                .extracting(Avatar::getName)
                .containsExactly("검은 고양이", "하얀 고양이", "노란 고양이");
        
        assertThat(defaultAvatars)
                .allMatch(Avatar::isDefault);
    }

    @Test
    @DisplayName("카테고리별 아바타 조회")
    void findByCategoryOrderBySortOrderAsc() {
        // when
        List<Avatar> catAvatars = avatarRepository.findByCategoryOrderBySortOrderAsc("CAT");
        List<Avatar> dogAvatars = avatarRepository.findByCategoryOrderBySortOrderAsc("DOG");

        // then
        assertThat(catAvatars).hasSize(3);
        assertThat(catAvatars)
                .extracting(Avatar::getCategory)
                .containsOnly("CAT");

        assertThat(dogAvatars).hasSize(1);
        assertThat(dogAvatars.get(0).getName()).isEqualTo("골든 리트리버");
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 조회 - 빈 리스트 반환")
    void findByCategory_NotFound() {
        // when
        List<Avatar> result = avatarRepository.findByCategoryOrderBySortOrderAsc("BIRD");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("아바타 저장 및 조회")
    void saveAndFindById() {
        // given
        Avatar newAvatar = Avatar.builder()
                .name("회색 늑대")
                .imageUrl("/images/avatars/wolf-grey.png")
                .description("멋진 회색 늑대")
                .isDefault(false)
                .sortOrder(5)
                .category("WOLF")
                .build();

        // when
        testEntityManager.persist(newAvatar);
        testEntityManager.flush();
        testEntityManager.clear();
        
        Avatar found = avatarRepository.findById(newAvatar.getId()).orElse(null);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("회색 늑대");
        assertThat(found.getImageUrl()).isEqualTo("/images/avatars/wolf-grey.png");
        assertThat(found.getCategory()).isEqualTo("WOLF");
        assertThat(found.isDefault()).isFalse();
    }

    @Test
    @DisplayName("정렬 순서 확인")
    void checkSortOrder() {
        // when
        List<Avatar> avatars = avatarRepository.findAllByOrderBySortOrderAsc();

        // then
        for (int i = 0; i < avatars.size() - 1; i++) {
            assertThat(avatars.get(i).getSortOrder())
                    .isLessThan(avatars.get(i + 1).getSortOrder());
        }
    }
}
