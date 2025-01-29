# -----------------------------------
# 1. Builder Stage (Gradle)
# -----------------------------------
FROM gradle:8.2.1-jdk17 AS builder

# Создадим директорию для сборки
WORKDIR /app

# Скопируем все файлы Gradle (wrapper) и основной проект
COPY . /app

# Выполним сборку (без тестов или с ними - на ваше усмотрение)
RUN gradle bootJar --no-daemon -x test

# -----------------------------------
# 2. Runtime Stage
# -----------------------------------
FROM eclipse-temurin:17-jdk-jammy
# Опционально можно взять другой базовый образ Java 17 (например, amazoncorretto:17, liberica, etc.)

WORKDIR /app

# Копируем собранный jar из Builder Stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# Запуск Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
