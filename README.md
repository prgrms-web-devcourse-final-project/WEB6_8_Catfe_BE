# Catfe

프로그래머스 백엔드 데브코스 6기 8회차 4차 프로젝트 - 5팀 Catfe
<br/>
<br/>

---

# ✅ PR #1: JWT 인증 통합 완료

## 📋 작업 요약
스터디룸 API에서 하드코딩된 사용자 ID를 제거하고, JWT 인증 시스템을 완전히 통합했습니다.

### 🔄 주요 변경 사항

#### 1. SecurityConfig 수정
- `/api/rooms/**` 경로의 `permitAll()` 제거
- 모든 방 API가 이제 JWT 인증 필수

#### 2. RoomController JWT 통합
- `CurrentUser` 의존성 주입으로 실제 사용자 ID 추출
- 하드코딩된 `Long currentUserId = 1L;` 제거 (10개 메서드)
- 불필요한 `@RequestHeader("Authorization")` 파라미터 제거

### 🔐 인증 흐름
1. **클라이언트 요청**: Authorization 헤더에 "Bearer {token}" 전달
2. **JwtAuthenticationFilter**: 토큰 추출 및 검증
3. **Controller**: `CurrentUser.getUserId()`로 사용자 ID 획득
4. **인증 실패 시**: 401 Unauthorized 자동 응답

### 🧪 테스트 방법
```bash
# 1. 로그인하여 JWT 토큰 받기
POST /api/auth/login
{
  "username": "user",
  "password": "password"
}

# 2. 토큰으로 방 생성
POST /api/rooms
Authorization: Bearer {받은_토큰}
{
  "title": "테스트 방",
  "isPrivate": false
}

# 3. 토큰 없이 요청 시 401 에러 확인
POST /api/rooms  # ❌ 401 Unauthorized
```

### 📊 영향받는 API 엔드포인트
| 엔드포인트 | 메서드 | 변경 사항 |
|-----------|--------|----------|
| `/api/rooms` | POST | JWT 인증 필수 |
| `/api/rooms/{roomId}/join` | POST | JWT 인증 필수 |
| `/api/rooms/{roomId}/leave` | POST | JWT 인증 필수 |
| `/api/rooms` | GET | JWT 인증 필수 |
| `/api/rooms/{roomId}` | GET | JWT 인증 필수 |
| `/api/rooms/my` | GET | JWT 인증 필수 |
| `/api/rooms/{roomId}` | PUT | JWT 인증 필수 |
| `/api/rooms/{roomId}` | DELETE | JWT 인증 필수 |
| `/api/rooms/{roomId}/members` | GET | JWT 인증 필수 |
| `/api/rooms/popular` | GET | JWT 인증 필수 |

---
<br/>
<br/>

# 개발 및 배포 프로세스 & Git 컨벤션 가이드
해당 프로젝트는 `dev` 브랜치에서 개발하고, `main`브랜치에서 배포합니다. <br/> <br/>
아래에 브랜치 전략, 커밋/PR 컨벤션, 워크플로우 전략, 브랜치 보호 규칙, 응답 데이터 및 예외처리 전략을 정리하였습니다. <br/> <br/>
개발 전에 꼭 읽어봐주세요! 
<br/>
<br/>

## 1. 브랜치 전략
- **`dev`**: 개발 브랜치
  - 모든 기능 개발은 feature 브랜치를 만들어 `dev`에 PR로 머지
  - 자동 브랜치 생성 및 Draft PR 대상 브랜치
  - 직접 push 및 외부 PR은 제한

- **`main`**: 배포 브랜치
  - 안정화된 코드를 머지하여 배포
  - `dev` → `main` PR은 관리자 혹은 릴리즈 담당자만 생성 및 승인 가능
  - 직접 push 및 외부 PR 제한
<br/>

## 2. 커밋/PR 컨벤션

### 커밋/PR 타입
- Feat : 새로운 기능 추가
- Fix : 버그 수정
- Env : 개발 환경 관련 설정
- Style : 코드 스타일 수정 (세미 콜론, 인덴트 등의 스타일적인 부분만)
- Refactor : 코드 리팩토링 (더 효율적인 코드로 변경 등)
- Design : CSS 등 디자인 추가/수정
- Comment : 주석 추가/수정
- Docs : 내부 문서 추가/수정
- Test : 테스트 추가/수정
- Chore : 빌드 관련 코드 수정
- Rename : 파일 및 폴더명 수정
- Remove : 파일 삭제

### 커밋/PR 메시지 양식
```
Feat: 로그인 함수 추가 -> 제목

로그인 요청을 위한 함수 구현 -> 본문
```
<br/>

## 3. 워크플로우 전략
```
1. 이슈 생성

  제목 양식 -> {commit Type}: {이슈내용}
  예시) Feat: 로그인 함수 추가

2. feature 브랜치 및 Github Projects item 자동생성

  자동으로 생성된 feature 브랜치 이름 (이슈 번호가 1번이라 가정) -> Feat/1

3. 해당 브랜치에서 작업 후 dev 브랜치에 PR 요청 (main 브랜치에는 직접 PR 금지)

4. PR 요청 시,

  - 모든 Status Check 통과 필요
  - 최소 2명 이상의 승인 필요

5. 승인받은 후 Squash & Merge 진행

6. Merge 후,

  - feature 브랜치 자동 삭제
  - 연관된 이슈 자동으로 닫힘
```


<br/>

## 4. 브랜치 보호 규칙

| 브랜치 | 보호 규칙 |
|--------|-----------|
| main   | 직접 push 금지, Force push 금지, 모든 CI 통과 필수, 관리자만 PR 가능 |
| dev    | 직접 push 금지, 리뷰 최소 2명 필수, 모든 CI 통과 필수 |

<br/>

## 5. 응답 데이터 및 예외처리 전략

### 서비스로부터 성공적으로 데이터를 받아온 경우

- 성공 응답 -> 컨트롤러에서 HTTP 상태 코드 + 응답 데이터(`RsData.success`) 반환

```java
// 컨트롤러 메서드 예시

@GetMapping("/api/v1/example")
public ResponseEntity<RsData<T>> example() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("message", data);
}
```
<br/>


```java
// 응답 데이터 예시 (json)

{
  "code": "SUCCESS_200",
  "message": "message",
  "data": {...} or {null},
  "success": true
}
```

<br/>

### 예외처리

- `CustomException()` 활용하여 예외를 던짐.
```java
// 예시 - findById() 사용할 때, 대상 엔티티가 존재하지 않는 경우

Example example = ExampleRepository
                    .findById(id)
                    .orElseThrow(() ->
                        new CustomException(ErrorCode.EXAMPLE_NOT_FOUND)
                    );
```


<br/>

- `GlobalExceptionHandler`에서 해당 예외를 처리 → HTTP 상태 코드 + 응답 데이터(`RsData.fail`) 반환
```java
// GlobalExceptionHandler의 CustomException 처리 메서드

@ExceptionHandler(CustomException.class)
public ResponseEntity<RsData<Void>> handleCustomException(
        CustomException ex
) {
    ErrorCode errorCode = ex.getErrorCode();

    return ResponseEntity
            .status(errorCode.getStatus())
            .body(RsData.fail(errorCode));
}
```

```java
// 응답 데이터 예시 (json)

{
  "code": ErrorCode.code,
  "message": ErrorCode.message,
  "data": {...} or {null},
  "success": false
}
```

<br/>

### 정리

```java
서비스(Service)
   └── 성공: 데이터 반환
   └── 실패: CustomException(ErrorCode) 던짐
        ↓
컨트롤러(Controller)
   └── 성공: HTTP 상태 + RsData.success 반환
   └── 실패: ControllerAdvice로 자동 처리
        ↓
GlobalExceptionHandler
   └── 예외 잡기 → HTTP 상태 + RsData.fail 반환
```







