package com.back.domain.board.comment.repository;

import java.util.Collection;
import java.util.List;

public interface CommentLikeRepositoryCustom {
    List<Long> findLikedCommentIdsIn(Long userId, Collection<Long> commentIds);
}
