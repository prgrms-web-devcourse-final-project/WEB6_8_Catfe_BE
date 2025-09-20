# Catfe

프로그래머스 백엔드 데브코스 6기 8회차 4차 프로젝트 - 5팀 Catfe
<br/>
<br/>

# 개발 및 배포 프로세스 & Git 컨벤션 가이드
해당 프로젝트는 `dev` 브랜치에서 개발하고, `main`브랜치에서 배포합니다. <br/> <br/>
아래에 브랜치 전략, 커밋/PR 컨벤션, Github 자동화 워크플로우, 브랜치 보호 규칙, 응답 데이터 및 예외처리 전략을 정리해두었습니다.

이것은 추가된 내용
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

## 3. Github 자동화 워크플로우
```
1. Github Projects 아이템 생성

제목 양식 -> {commit Type}: {이슈내용}
예시) Feat: 로그인 함수 추가

2. 자동 이슈 생성 (이슈 제목은 1번에서 만든 제목과 동일하다.)

3. 자동으로 생성된 feature 브랜치 이름 (이슈 번호가 1번이라 가정)
Feat/1
```
<br/>

## 4. 브랜치 보호 규칙

| 브랜치 | 보호 규칙 |
|--------|-----------|
| main   | 직접 push 금지, Force push 금지, 모든 CI 통과 필수, 관리자만 PR 가능 |
| dev    | 직접 push 금지, 리뷰 최소 2명 필수, 모든 CI 통과 필수 |

<br/>

## 5. 전체 요약
1. 작업 전, Github Projects에 item 추가. 이슈 자동 생성.
2. 생성한 이슈 제목 기반으로 feature 브랜치가 자동 생성
3. 해당 브랜치에서 작업 후, dev 브랜치에 PR 요청
4. PR 요청 시, 빌드 & 테스트(status check) 통과 후, 2명 이상의 승인 필요
5. 승인받은 후, Squash & merge 진행
6. merge 후, 브랜치 자동 삭제 및 이슈가 자동으로 닫힘.

<br/>

## 6. 응답 데이터 및 예외처리 전략

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







