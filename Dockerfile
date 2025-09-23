# 첫 번째 스테이지 : 빌드 스테이지
FROM gradle:jdk-21-and-23-graal-jammy AS builder

# 작업용 디렉토리 설명
WORKDIR /app

# Gradle 래퍼 복사
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN gradle dependencies --no-daemon

# 소스코드 복사
COPY src src

# .env 복사
COPY .env .env

# 애플리케이션 빌드
RUN gradle build --no-daemon

# 두 번째 스테이지 : 실행 스테이지
FROM container-registry.oracle.com/graalvm/jdk:21

WORKDIR /app

# 첫 번째 스테이지에서 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/.env .env

# 실행할 JAR 파일 지정
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]