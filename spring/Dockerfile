# Stage 1: Build
FROM amazoncorretto:17-alpine AS builder
WORKDIR /app

# 의존성 레이어 캐시 (build.gradle이 바뀌지 않으면 재사용)
COPY gradlew .
COPY gradle gradle/
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

COPY src src/
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM amazoncorretto:17-alpine
WORKDIR /app

# 보안: non-root 유저로 실행
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=builder /app/build/libs/app.jar app.jar

# 작은 인스턴스(2GB)에서 MySQL·Node와 함께 쓰도록 힙 상한 지정
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseSerialGC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
