package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomChatMessageRepository extends JpaRepository<RoomChatMessage, Long>, RoomChatMessageRepositoryCustom {
}
