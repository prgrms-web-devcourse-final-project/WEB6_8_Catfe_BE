package com.back.domain.file.util;

import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.file.entity.EntityType;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * EntityType, EntityId를 통해 매핑되는 데이터 존재 확인
 */
@Component
@RequiredArgsConstructor
public class EntityValidator {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public void validate(EntityType entityType, Long entityId) {
        switch (entityType) {
            case POST:
                if(!postRepository.existsById(entityId)) throw new CustomException(ErrorCode.POST_NOT_FOUND);
                break;

            case COMMENT:
                if(!commentRepository.existsById(entityId)) throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
                break;
        }
    }
}
