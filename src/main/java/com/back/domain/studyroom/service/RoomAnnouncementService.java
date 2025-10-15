package com.back.domain.studyroom.service;

import com.back.domain.studyroom.dto.RoomAnnouncementResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomAnnouncement;
import com.back.domain.studyroom.repository.RoomAnnouncementRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 방 공지사항 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomAnnouncementService {
    
    private final RoomAnnouncementRepository announcementRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    
    /**
     * 공지사항 생성 (방장만 가능)
     */
    @Transactional
    public RoomAnnouncementResponse createAnnouncement(Long roomId, String title, String content, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 방장 권한 확인
        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        RoomAnnouncement announcement = RoomAnnouncement.create(room, user, title, content);
        RoomAnnouncement saved = announcementRepository.save(announcement);
        
        log.info("공지사항 생성 - RoomId: {}, AnnouncementId: {}, UserId: {}", roomId, saved.getId(), userId);
        
        return RoomAnnouncementResponse.from(saved);
    }
    
    /**
     * 공지사항 수정 (방장만 가능)
     */
    @Transactional
    public RoomAnnouncementResponse updateAnnouncement(Long announcementId, String title, String content, Long userId) {
        RoomAnnouncement announcement = announcementRepository.findByIdWithCreator(announcementId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        
        Room room = announcement.getRoom();
        
        // 방장 권한 확인
        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        
        announcement.update(title, content);
        
        log.info("공지사항 수정 - AnnouncementId: {}, UserId: {}", announcementId, userId);
        
        return RoomAnnouncementResponse.from(announcement);
    }
    
    /**
     * 공지사항 삭제 (방장만 가능)
     */
    @Transactional
    public void deleteAnnouncement(Long announcementId, Long userId) {
        RoomAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        
        Room room = announcement.getRoom();
        
        // 방장 권한 확인
        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        
        announcementRepository.delete(announcement);
        
        log.info("공지사항 삭제 - AnnouncementId: {}, UserId: {}", announcementId, userId);
    }
    
    /**
     * 공지사항 목록 조회 (핀 고정 우선, 최신순)
     */
    public List<RoomAnnouncementResponse> getAnnouncements(Long roomId) {
        // 방 존재 확인
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }
        
        List<RoomAnnouncement> announcements = announcementRepository.findByRoomIdWithCreator(roomId);
        
        return announcements.stream()
                .map(RoomAnnouncementResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 공지사항 단건 조회
     */
    public RoomAnnouncementResponse getAnnouncement(Long announcementId) {
        RoomAnnouncement announcement = announcementRepository.findByIdWithCreator(announcementId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        
        return RoomAnnouncementResponse.from(announcement);
    }
    
    /**
     * 공지사항 핀 고정/해제 토글 (방장만 가능)
     */
    @Transactional
    public RoomAnnouncementResponse togglePin(Long announcementId, Long userId) {
        RoomAnnouncement announcement = announcementRepository.findByIdWithCreator(announcementId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        
        Room room = announcement.getRoom();
        
        // 방장 권한 확인
        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        
        announcement.togglePin();
        
        log.info("공지사항 핀 토글 - AnnouncementId: {}, isPinned: {}, UserId: {}", 
                announcementId, announcement.isPinned(), userId);
        
        return RoomAnnouncementResponse.from(announcement);
    }
}
