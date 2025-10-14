package com.back.domain.study.memo.service;

import com.back.domain.study.memo.dto.MemoRequestDto;
import com.back.domain.study.memo.dto.MemoResponseDto;
import com.back.domain.study.memo.entity.Memo;
import com.back.domain.study.memo.repository.MemoRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoService {
    private final MemoRepository memoRepository;
    private final UserRepository userRepository;

    // ==================== 생성 및 수정 ===================
    // 같은 날짜에 메모가 있으면 수정, 없으면 생성
    @Transactional
    public MemoResponseDto createOrUpdateMemo(Long userId, MemoRequestDto request) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 같은 날짜의 메모가 있는지 확인
        Optional<Memo> existingMemo = memoRepository.findByUserIdAndDate(userId, request.getDate());

        Memo memo;
        if (existingMemo.isPresent()) {
            // 기존 메모 수정
            memo = existingMemo.get();
            memo.update(request.getDescription());
        } else {
            // 새 메모 생성
            memo = Memo.create(user, request.getDate(), request.getDescription());
            memo = memoRepository.save(memo);
        }

        return MemoResponseDto.from(memo);
    }

    // ==================== 조회 ===================
    public MemoResponseDto getMemoByDate(Long userId, LocalDate date) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 메모를 조회해서 해당 날짜에 없으면 null
        return memoRepository.findByUserIdAndDate(userId, date)
                .map(MemoResponseDto::from)
                .orElse(null);
    }

    // ==================== 삭제 ===================
    @Transactional
    public MemoResponseDto deleteMemo(Long userId, Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMO_NOT_FOUND));

        // 권한 확인
        if (!memo.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        MemoResponseDto memoDto = MemoResponseDto.from(memo);

        memoRepository.delete(memo);
        // 삭제된 메모 정보 반환
        return memoDto;
    }

}
