-- =========================
-- SUBJECT 카테고리
-- =========================
INSERT INTO post_category (name, type, created_at, updated_at)
VALUES
    ('프론트엔드', 'SUBJECT', NOW(), NOW()),
    ('백엔드', 'SUBJECT', NOW(), NOW()),
    ('CS', 'SUBJECT', NOW(), NOW()),
    ('알고리즘', 'SUBJECT', NOW(), NOW()),
    ('데이터 사이언스', 'SUBJECT', NOW(), NOW()),
    ('인공지능', 'SUBJECT', NOW(), NOW()),
    ('클라우드', 'SUBJECT', NOW(), NOW()),
    ('보안', 'SUBJECT', NOW(), NOW()),
    ('데브옵스', 'SUBJECT', NOW(), NOW()),
    ('영어', 'SUBJECT', NOW(), NOW()),
    ('영어 회화', 'SUBJECT', NOW(), NOW()),
    ('토익', 'SUBJECT', NOW(), NOW()),
    ('토플', 'SUBJECT', NOW(), NOW()),
    ('아이엘츠', 'SUBJECT', NOW(), NOW()),
    ('일본어', 'SUBJECT', NOW(), NOW()),
    ('JLPT', 'SUBJECT', NOW(), NOW()),
    ('중국어', 'SUBJECT', NOW(), NOW()),
    ('HSK', 'SUBJECT', NOW(), NOW()),
    ('스페인어', 'SUBJECT', NOW(), NOW()),
    ('프랑스어', 'SUBJECT', NOW(), NOW()),
    ('독일어', 'SUBJECT', NOW(), NOW()),
    ('수학', 'SUBJECT', NOW(), NOW()),
    ('확률통계', 'SUBJECT', NOW(), NOW()),
    ('물리', 'SUBJECT', NOW(), NOW()),
    ('화학', 'SUBJECT', NOW(), NOW()),
    ('생명과학', 'SUBJECT', NOW(), NOW()),
    ('천문', 'SUBJECT', NOW(), NOW()),
    ('역사', 'SUBJECT', NOW(), NOW()),
    ('철학', 'SUBJECT', NOW(), NOW()),
    ('심리학', 'SUBJECT', NOW(), NOW()),
    ('사회', 'SUBJECT', NOW(), NOW()),
    ('경제', 'SUBJECT', NOW(), NOW()),
    ('글쓰기', 'SUBJECT', NOW(), NOW()),
    ('에세이', 'SUBJECT', NOW(), NOW()),
    ('독서', 'SUBJECT', NOW(), NOW()),
    ('속독', 'SUBJECT', NOW(), NOW()),
    ('프레젠테이션', 'SUBJECT', NOW(), NOW()),
    ('스피치', 'SUBJECT', NOW(), NOW()),
    ('논술', 'SUBJECT', NOW(), NOW()),
    ('자기계발', 'SUBJECT', NOW(), NOW()),
    ('음악이론', 'SUBJECT', NOW(), NOW()),
    ('피아노', 'SUBJECT', NOW(), NOW()),
    ('기타', 'SUBJECT', NOW(), NOW()),
    ('드로잉', 'SUBJECT', NOW(), NOW()),
    ('수채화', 'SUBJECT', NOW(), NOW()),
    ('디지털아트', 'SUBJECT', NOW(), NOW()),
    ('사진', 'SUBJECT', NOW(), NOW()),
    ('영상편집', 'SUBJECT', NOW(), NOW()),
    ('요리', 'SUBJECT', NOW(), NOW()),
    ('제과제빵', 'SUBJECT', NOW(), NOW()),
    ('바리스타', 'SUBJECT', NOW(), NOW()),
    ('정보처리기사', 'SUBJECT', NOW(), NOW()),
    ('컴활', 'SUBJECT', NOW(), NOW()),
    ('MOS', 'SUBJECT', NOW(), NOW()),
    ('한국사능력검정', 'SUBJECT', NOW(), NOW()),
    ('교원임용', 'SUBJECT', NOW(), NOW()),
    ('회계관리', 'SUBJECT', NOW(), NOW()),
    ('전산회계', 'SUBJECT', NOW(), NOW()),
    ('GTQ', 'SUBJECT', NOW(), NOW()),
    ('마케팅', 'SUBJECT', NOW(), NOW()),
    ('브랜딩', 'SUBJECT', NOW(), NOW()),
    ('서비스기획', 'SUBJECT', NOW(), NOW()),
    ('UX/UI', 'SUBJECT', NOW(), NOW()),
    ('제품디자인', 'SUBJECT', NOW(), NOW()),
    ('재테크', 'SUBJECT', NOW(), NOW()),
    ('부동산', 'SUBJECT', NOW(), NOW()),
    ('주식', 'SUBJECT', NOW(), NOW()),
    ('코칭', 'SUBJECT', NOW(), NOW()),
    ('명상', 'SUBJECT', NOW(), NOW()),
    ('기타 과목', 'SUBJECT', NOW(), NOW());

-- =========================
-- DEMOGRAPHIC 카테고리
-- =========================
INSERT INTO post_category (name, type, created_at, updated_at)
VALUES
    ('중학생', 'DEMOGRAPHIC', NOW(), NOW()),
    ('고등학생', 'DEMOGRAPHIC', NOW(), NOW()),
    ('대학생', 'DEMOGRAPHIC', NOW(), NOW()),
    ('취준생', 'DEMOGRAPHIC', NOW(), NOW()),
    ('직장인', 'DEMOGRAPHIC', NOW(), NOW()),
    ('기타', 'DEMOGRAPHIC', NOW(), NOW());

-- =========================
-- GROUP_SIZE 카테고리
-- =========================
INSERT INTO post_category (name, type, created_at, updated_at)
VALUES
    ('2~4명', 'GROUP_SIZE', NOW(), NOW()),
    ('5~10명', 'GROUP_SIZE', NOW(), NOW()),
    ('10~20명', 'GROUP_SIZE', NOW(), NOW());

-- =========================
-- AVATAR 초기 데이터 (고양이 아바타 3개 - 기본 랜덤하기 위해 배정)
-- =========================
INSERT INTO avatars (id, name, image_url, description, is_default, sort_order, category, created_at, updated_at)
VALUES
    (1, '검은 고양이', '/images/avatars/cat-black.png', '귀여운 검은 고양이', true, 1, 'CAT', NOW(), NOW()),
    (2, '하얀 고양이', '/images/avatars/cat-white.png', '우아한 하얀 고양이', true, 2, 'CAT', NOW(), NOW()),
    (3, '노란 고양이', '/images/avatars/cat-orange.png', '발랄한 노란 고양이', true, 3, 'CAT', NOW(), NOW());
