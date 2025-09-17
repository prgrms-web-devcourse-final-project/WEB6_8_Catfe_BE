# Catfe

프로그래머스 백엔드 데브코스 6기 8회차 4차 프로젝트

## 협업규칙
1. 작업 전, Github Project에 item을 추가. 이슈가 자동으로 생성. (이슈 제목 작성 방식은 아래 방식을 참고.)
2. 생성한 이슈 제목을 기반으로 브런치가 자동 생성
3. 해당 브런치에서 작업 후, dev 브런치에 PR 요청 (main 브런치 x)
4. PR 요청 시, 빌드 & 테스트(status check) 통과 후, 2명 이상의 승인 필요
5. 승인받은 후, Squash & merge 진행
6. merge 후에는 브런치를 삭제해주시고, `git fetch --prune`을 통해 로컬에 남아있는 원격 레포지토리를 정리해주세요.

## Github Project item 작성 방식 및 이후 절차
```
1. Github Projects 아이템 제목 작성
작성양식 -> {commit Type}: 작업내용
예시) Feat: 로그인 함수 추가

2. 생성된 이슈 이름
Feat: 로그인 함수 추가

3. 자동으로 생성된 브런치 이름 
Feat/1
```

## Commit Message Convention
### 커밋 메시지 양식
```
Feat: "로그인 함수 추가" -> 제목

로그인 요청을 위한 함수 구현 -> 본문
...
```

### Commit Type
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
