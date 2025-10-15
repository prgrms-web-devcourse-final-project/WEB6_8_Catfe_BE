package com.back.domain.studyroom.service;

import com.back.domain.studyroom.dto.*;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomGuestbook;
import com.back.domain.studyroom.entity.RoomGuestbookPin;
import com.back.domain.studyroom.entity.RoomGuestbookReaction;
import com.back.domain.studyroom.repository.RoomGuestbookPinRepository;
import com.back.domain.studyroom.repository.RoomGuestbookReactionRepository;
import com.back.domain.studyroom.repository.RoomGuestbookRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomGuestbookService {

    private final RoomGuestbookRepository guestbookRepository;
    private final RoomGuestbookReactionRepository reactionRepository;
    private final RoomGuestbookPinRepository pinRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    /**
     * 방명록 생성
     */
    @Transactional
    public GuestbookResponse createGuestbook(Long roomId, String content, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RoomGuestbook guestbook = RoomGuestbook.create(room, user, content);
        RoomGuestbook savedGuestbook = guestbookRepository.save(guestbook);

        log.info("방명록 생성 완료 - RoomId: {}, UserId: {}, GuestbookId: {}", 
                roomId, userId, savedGuestbook.getId());

        return GuestbookResponse.from(savedGuestbook, userId, Collections.emptyList(), false);
    }

    /**
     * 방명록 목록 조회 (페이징, 이모지 반응 포함, 핀 우선 정렬)
     * 로그인한 사용자가 핀한 방명록이 최상단에 표시됨
     */
    public Page<GuestbookResponse> getGuestbooks(Long roomId, Long currentUserId, Pageable pageable) {
        // 방 존재 확인
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }

        // 핀을 고려한 방명록 목록 조회 (로그인 사용자만 핀 정렬 적용)
        Page<RoomGuestbook> guestbooks;
        if (currentUserId != null) {
            guestbooks = guestbookRepository.findByRoomIdWithUserOrderByPin(roomId, currentUserId, pageable);
        } else {
            // 비로그인 사용자는 일반 정렬
            guestbooks = guestbookRepository.findByRoomIdWithUser(roomId, pageable);
        }

        // 방명록 ID 목록 추출
        List<Long> guestbookIds = guestbooks.getContent().stream()
                .map(RoomGuestbook::getId)
                .collect(Collectors.toList());

        // 현재 사용자가 핀한 방명록 ID Set (로그인 사용자만)
        Set<Long> pinnedGuestbookIds = currentUserId != null
                ? pinRepository.findPinnedGuestbookIdsByUserIdAndRoomId(currentUserId, roomId)
                : Collections.emptySet();

        // 모든 방명록의 반응 일괄 조회 (N+1 방지)
        Map<Long, List<RoomGuestbookReaction>> reactionsMap = new HashMap<>();
        if (!guestbookIds.isEmpty()) {
            for (Long guestbookId : guestbookIds) {
                List<RoomGuestbookReaction> reactions = reactionRepository.findByGuestbookIdWithUser(guestbookId);
                reactionsMap.put(guestbookId, reactions);
            }
        }

        return guestbooks.map(guestbook -> {
            List<RoomGuestbookReaction> reactions = reactionsMap.getOrDefault(guestbook.getId(), Collections.emptyList());
            List<GuestbookReactionSummary> reactionSummaries = buildReactionSummaries(reactions, currentUserId);
            boolean isPinned = pinnedGuestbookIds.contains(guestbook.getId());
            return GuestbookResponse.from(guestbook, currentUserId, reactionSummaries, isPinned);
        });
    }

    /**
     * 방명록 단건 조회
     */
    public GuestbookResponse getGuestbook(Long guestbookId, Long currentUserId) {
        RoomGuestbook guestbook = guestbookRepository.findByIdWithUserAndRoom(guestbookId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUESTBOOK_NOT_FOUND));

        List<RoomGuestbookReaction> reactions = reactionRepository.findByGuestbookIdWithUser(guestbookId);
        List<GuestbookReactionSummary> reactionSummaries = buildReactionSummaries(reactions, currentUserId);
        
        // 핀 여부 확인
        boolean isPinned = currentUserId != null && 
                pinRepository.findByGuestbookIdAndUserId(guestbookId, currentUserId).isPresent();

        return GuestbookResponse.from(guestbook, currentUserId, reactionSummaries, isPinned);
    }

    /**
     * 방명록 수정 (작성자만 가능)
     */
    @Transactional
    public GuestbookResponse updateGuestbook(Long guestbookId, String content, Long userId) {
        RoomGuestbook guestbook = guestbookRepository.findByIdWithUserAndRoom(guestbookId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUESTBOOK_NOT_FOUND));

        // 작성자 권한 확인
        if (!guestbook.isAuthor(userId)) {
            throw new CustomException(ErrorCode.GUESTBOOK_NO_PERMISSION);
        }

        guestbook.updateContent(content);

        List<RoomGuestbookReaction> reactions = reactionRepository.findByGuestbookIdWithUser(guestbookId);
        List<GuestbookReactionSummary> reactionSummaries = buildReactionSummaries(reactions, userId);
        
        // 핀 여부 확인
        boolean isPinned = pinRepository.findByGuestbookIdAndUserId(guestbookId, userId).isPresent();

        log.info("방명록 수정 완료 - GuestbookId: {}, UserId: {}", guestbookId, userId);

        return GuestbookResponse.from(guestbook, userId, reactionSummaries, isPinned);
    }

    /**
     * 방명록 삭제 (작성자만 가능)
     */
    @Transactional
    public void deleteGuestbook(Long guestbookId, Long userId) {
        RoomGuestbook guestbook = guestbookRepository.findByIdWithUserAndRoom(guestbookId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUESTBOOK_NOT_FOUND));

        // 작성자 권한 확인
        if (!guestbook.isAuthor(userId)) {
            throw new CustomException(ErrorCode.GUESTBOOK_NO_PERMISSION);
        }

        guestbookRepository.delete(guestbook);

        log.info("방명록 삭제 완료 - GuestbookId: {}, UserId: {}", guestbookId, userId);
    }

    /**
     * 방명록 이모지 반응 추가/제거 토글
     * - 이미 반응한 이모지면 제거
     * - 반응하지 않은 이모지면 추가
     */
    @Transactional
    public GuestbookResponse toggleReaction(Long guestbookId, String emoji, Long userId) {
        RoomGuestbook guestbook = guestbookRepository.findByIdWithUserAndRoom(guestbookId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUESTBOOK_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 반응 확인
        Optional<RoomGuestbookReaction> existingReaction = 
                reactionRepository.findByGuestbookIdAndUserIdAndEmoji(guestbookId, userId, emoji);

        if (existingReaction.isPresent()) {
            // 이미 반응한 경우 → 제거
            reactionRepository.delete(existingReaction.get());
            log.info("방명록 반응 제거 - GuestbookId: {}, UserId: {}, Emoji: {}", 
                    guestbookId, userId, emoji);
        } else {
            // 반응하지 않은 경우 → 추가
            RoomGuestbookReaction reaction = RoomGuestbookReaction.create(guestbook, user, emoji);
            reactionRepository.save(reaction);
            log.info("방명록 반응 추가 - GuestbookId: {}, UserId: {}, Emoji: {}", 
                    guestbookId, userId, emoji);
        }

        // 업데이트된 반응 목록 조회
        List<RoomGuestbookReaction> reactions = reactionRepository.findByGuestbookIdWithUser(guestbookId);
        List<GuestbookReactionSummary> reactionSummaries = buildReactionSummaries(reactions, userId);
        
        // 핀 여부 확인
        boolean isPinned = pinRepository.findByGuestbookIdAndUserId(guestbookId, userId).isPresent();

        return GuestbookResponse.from(guestbook, userId, reactionSummaries, isPinned);
    }

    /**
     * 방명록 핀 추가/제거 토글
     * - 이미 핀한 방명록이면 제거
     * - 핀하지 않은 방명록이면 추가
     */
    @Transactional
    public GuestbookResponse togglePin(Long guestbookId, Long userId) {
        RoomGuestbook guestbook = guestbookRepository.findByIdWithUserAndRoom(guestbookId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUESTBOOK_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 핀 확인
        Optional<RoomGuestbookPin> existingPin = 
                pinRepository.findByGuestbookIdAndUserId(guestbookId, userId);

        boolean isPinned;
        if (existingPin.isPresent()) {
            // 이미 핀한 경우 → 제거
            pinRepository.delete(existingPin.get());
            isPinned = false;
            log.info("방명록 핀 제거 - GuestbookId: {}, UserId: {}", guestbookId, userId);
        } else {
            // 핀하지 않은 경우 → 추가
            RoomGuestbookPin pin = RoomGuestbookPin.create(guestbook, user);
            pinRepository.save(pin);
            isPinned = true;
            log.info("방명록 핀 추가 - GuestbookId: {}, UserId: {}", guestbookId, userId);
        }

        // 반응 목록 조회
        List<RoomGuestbookReaction> reactions = reactionRepository.findByGuestbookIdWithUser(guestbookId);
        List<GuestbookReactionSummary> reactionSummaries = buildReactionSummaries(reactions, userId);

        return GuestbookResponse.from(guestbook, userId, reactionSummaries, isPinned);
    }

    /**
     * 이모지 반응 요약 정보 생성
     * 이모지별로 그룹화하여 개수와 반응한 사용자 정보 포함
     */
    private List<GuestbookReactionSummary> buildReactionSummaries(
            List<RoomGuestbookReaction> reactions, Long currentUserId) {
        
        if (reactions.isEmpty()) {
            return Collections.emptyList();
        }

        // 이모지별로 그룹화
        Map<String, List<RoomGuestbookReaction>> groupedByEmoji = reactions.stream()
                .collect(Collectors.groupingBy(RoomGuestbookReaction::getEmoji));

        return groupedByEmoji.entrySet().stream()
                .map(entry -> {
                    String emoji = entry.getKey();
                    List<RoomGuestbookReaction> emojiReactions = entry.getValue();

                    // 현재 사용자가 이 이모지로 반응했는지
                    boolean reactedByMe = currentUserId != null && 
                            emojiReactions.stream()
                                    .anyMatch(r -> r.isReactedBy(currentUserId));

                    // 최근 반응한 사용자 닉네임 (최대 3명)
                    List<String> recentUsers = emojiReactions.stream()
                            .limit(3)
                            .map(r -> r.getUser().getNickname())
                            .collect(Collectors.toList());

                    return GuestbookReactionSummary.builder()
                            .emoji(emoji)
                            .count((long) emojiReactions.size())
                            .reactedByMe(reactedByMe)
                            .recentUsers(recentUsers)
                            .build();
                })
                .sorted(Comparator.comparing(GuestbookReactionSummary::getCount).reversed())
                .collect(Collectors.toList());
    }
}
