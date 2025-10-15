package com.back.domain.studyroom.service;

import com.back.domain.studyroom.dto.RoomFavoriteResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomFavorite;
import com.back.domain.studyroom.repository.RoomFavoriteRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.service.RoomParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 방 즐겨찾기 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomFavoriteService {

    private final RoomFavoriteRepository favoriteRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomParticipantService roomParticipantService;

    /**
     * 즐겨찾기 추가
     */
    @Transactional
    public void addFavorite(Long roomId, Long userId) {
        // 이미 즐겨찾기 되어있는지 확인
        if (favoriteRepository.existsByUserIdAndRoomId(userId, roomId)) {
            log.info("이미 즐겨찾기된 방 - UserId: {}, RoomId: {}", userId, roomId);
            return; // Idempotent: 이미 있으면 무시
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RoomFavorite favorite = RoomFavorite.create(user, room);
        favoriteRepository.save(favorite);

        log.info("즐겨찾기 추가 - UserId: {}, RoomId: {}", userId, roomId);
    }

    /**
     * 즐겨찾기 제거
     */
    @Transactional
    public void removeFavorite(Long roomId, Long userId) {
        RoomFavorite favorite = favoriteRepository.findByUserIdAndRoomId(userId, roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        favoriteRepository.delete(favorite);

        log.info("즐겨찾기 제거 - UserId: {}, RoomId: {}", userId, roomId);
    }

    /**
     * 내 즐겨찾기 목록 조회
     */
    public List<RoomFavoriteResponse> getMyFavorites(Long userId) {
        List<RoomFavorite> favorites = favoriteRepository.findByUserIdWithRoom(userId);

        if (favorites.isEmpty()) {
            return List.of();
        }

        // 방 ID 리스트 추출
        List<Long> roomIds = favorites.stream()
                .map(f -> f.getRoom().getId())
                .collect(Collectors.toList());

        // Redis에서 참가자 수 일괄 조회 (N+1 방지)
        Map<Long, Long> participantCounts = roomParticipantService.getParticipantCounts(roomIds);

        // 응답 생성
        return favorites.stream()
                .map(favorite -> RoomFavoriteResponse.of(
                        favorite.getRoom(),
                        participantCounts.getOrDefault(favorite.getRoom().getId(), 0L),
                        favorite.getCreatedAt()  // 즐겨찾기한 시간
                ))
                .collect(Collectors.toList());
    }

    /**
     * 특정 방의 즐겨찾기 여부 확인
     */
    public boolean isFavorite(Long roomId, Long userId) {
        return favoriteRepository.existsByUserIdAndRoomId(userId, roomId);
    }

    /**
     * 여러 방의 즐겨찾기 여부 일괄 확인 (N+1 방지)
     */
    public Set<Long> getFavoriteRoomIds(List<Long> roomIds, Long userId) {
        return favoriteRepository.findFavoriteRoomIds(userId, roomIds);
    }
}
