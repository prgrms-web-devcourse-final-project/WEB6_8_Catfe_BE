package com.back.domain.board.comment.repository.custom;

import java.util.Collection;
import java.util.List;

public interface CommentLikeRepositoryCustom {
    List<Long> findLikedCommentIdsIn(Long userId, Collection<Long> commentIds);
}
