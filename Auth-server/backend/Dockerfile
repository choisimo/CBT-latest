# ================= STAGE 1: Build =================
FROM gradle:8.7-jdk21 AS build

# 작업 디렉토리 설정
WORKDIR /home/gradle/src

# --- 캐시 최적화를 위한 단계 ---
# 1. 빌드에 필요한 Gradle 파일들을 먼저 복사합니다.
#    (context가 현재 폴더이므로 경로가 간단해집니다.)
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle/

# 2. 의존성만 먼저 다운로드하여 Docker 캐시 레이어를 생성합니다.
RUN ./gradlew dependencies --no-daemon

# 3. 소스 코드를 복사합니다.
COPY src ./src

# 4. 애플리케이션을 빌드합니다.
RUN ./gradlew build -x test --no-daemon

# ================= STAGE 2: Run =================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Build 스테이지에서 생성된 JAR 파일을 복사합니다.
# (경로가 간단해집니다.)
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

EXPOSE 7078

ENTRYPOINT ["java", "-jar", "app.jar"]
