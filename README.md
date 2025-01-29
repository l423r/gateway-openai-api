# Gateway OpenAI API

Проект **Gateway OpenAI API** — это Spring Cloud Gateway-сервис для проксирования запросов к OpenAI (ChatGPT, GPT-4 и т.д.). Он служит «шлюзом», который располагается в разрешённой зоне (DMZ), перенаправляя запросы из закрытого периметра в публичный OpenAI API.

## Содержание
1. [Особенности проекта](#особенности-проекта)
2. [Требования](#требования)
3. [Сборка и запуск](#сборка-и-запуск)
    - [Локальный запуск (Gradle)](#локальный-запуск-gradle)
    - [Сборка и запуск Docker-образа](#сборка-и-запуск-docker-образа)
4. [Конфигурация](#конфигурация)
5. [Пример использования](#пример-использования)
6. [Логирование](#логирование)
7. [Структура проекта](#структура-проекта)
8. [Дополнительно](#дополнительно)

---

## Особенности проекта
- Использует **Java 17** и **Spring Boot 3**
- Включает **Spring Cloud Gateway** для маршрутизации трафика к OpenAI API ( https://api.openai.com )
- Может добавлять или не добавлять заголовок `Authorization: Bearer <API_KEY>` в зависимости от настроек
- Удобно масштабируется и настраивается через `application.yml`
- Возможен **multi-stage Docker build** (Gradle + Temurin JDK 17)

---

## Требования
- **Java 17** (или более поздняя)
- **Gradle** (если вы не используете Gradle Wrapper)
- Docker (опционально, если планируете запуск в контейнере)

---

## Сборка и запуск

### Локальный запуск (Gradle)

1. Клонируйте репозиторий:
   ```bash
   git clone <repo_url> gateway-openai-api
   cd gateway-openai-api
   ```
2. Соберите проект:
   ```bash
   ./gradlew clean build
   ```
3. Запустите приложение:
   ```bash
   ./gradlew bootRun
   ```
4. Приложение по умолчанию слушает на порту `8080`. Если не изменяли настройки, доступно по адресу:
   ```
   http://localhost:8080
   ```

### Сборка и запуск Docker-образа

1. Соберите Docker-образ:
   ```bash
   docker build -t gateway-openai-api:1.0 .
   ```
2. Запустите контейнер:
   ```bash
   docker run -d -p 8080:8080 --name gateway-openai-api gateway-openai-api:1.0
   ```
3. Теперь приложение доступно на `http://localhost:8080`.

---

## Конфигурация

Основные настройки определяются в `application.yml` (находится в `src/main/resources`):

- **Порт**: `server.port` (по умолчанию `8080`).
- **Маршруты** (Spring Cloud Gateway Routes):
    - Пример маршрута для OpenAI:
      ```yaml
      spring:
        cloud:
          gateway:
            routes:
              - id: openai-route
                uri: https://api.openai.com/v1
                predicates:
                  - Path=/openai/**
                filters:
                  - StripPrefix=1
      ```
    - Этот маршрут перенаправляет любой запрос `http://<gateway_host>:8080/openai/...` к `https://api.openai.com/v1/...`, убирая префикс `/openai`.
- **Логирование**: настраивается через секцию `logging.level` в `application.yml` или в `logback-spring.xml`.

> **Важно**: Если требуется добавлять/менять заголовок `Authorization`, настройте соответствующий фильтр (например, `OpenAiAuthFilter`).

---

## Пример использования

- Если ваше внутреннее приложение может обращаться только к адресу гейтвея, вы будете отправлять запросы вида:
  ```
  POST http://gateway-host:8080/openai/chat/completions
  Content-Type: application/json
  Authorization: Bearer <OPENAI_API_KEY or something>
  {
    "model": "gpt-3.5-turbo",
    "messages": [ ... ]
  }
  ```
- Гейтвей перенаправит запрос на `https://api.openai.com/v1/chat/completions`, сохранив заголовок `Authorization` (или добавив его, если вы используете специальный фильтр).

---

## Логирование

- В проекте может использоваться **GlobalFilter** (например, `LoggingGlobalFilter`) для вывода в лог:
    - Адреса входящего запроса
    - HTTP-метода
    - Кода ответа
- Чтобы включить/выключить подробные логи, отредактируйте `application.yml`:
  ```yaml
  logging:
    level:
      root: INFO
      org.springframework.cloud.gateway: DEBUG
      reactor.netty: INFO
  ```
- Для гибкой настройки формата логов используйте `logback-spring.xml` в `src/main/resources`.

---

## Структура проекта

```
gateway-openai-api
├─ gradle/                 # служебные файлы Gradle
├─ build/                  # автоматически создаётся при сборке
├─ src/
│  └─ main/
│     ├─ java/
│     │  └─ ru/svolyrk/gatewayopenaiapi/
│     │     ├─ GatewayOpenaiApiApplication.java   # Основной класс Spring Boot
│     │     └─ config/
│     │        └─ LoggingGlobalFilter.java        # Пример глобального фильтра для логирования
│     └─ resources/
│        ├─ application.yml                       # Конфигурация Spring Boot/Cloud Gateway
│        └─ logback-spring.xml                    # (Опционально) расширенная конфигурация логов
└─ Dockerfile                                      # Multi-stage сборка (Gradle + Temurin 17)
└─ build.gradle                                    # Сборка Gradle
└─ gradlew / gradlew.bat                          # Gradle Wrapper
```

- **GatewayOpenaiApiApplication** — точка входа (Spring Boot).
- **application.yml** — основные настройки.
- **LoggingGlobalFilter** — пример фильтра, логирующего запросы.

---

## Дополнительно

- **Безопасность**: При необходимости можно добавить аутентификацию, авторизацию или ограничение доступа по IP-адресам.
- **Настройка CORS**: Если нужно, можно настроить глобальный CORS в `application.yml` для доступа из браузеров.
- **Vault / Secrets**: Рекомендуется хранить ключи и пароли (например, ключ OpenAI) в безопасном хранилище (Vault, Kubernetes Secrets и др.) вместо конфигурационных файлов.
- **Масштабирование**: Приложение можно легко запускать в Kubernetes или других контейнерных оркестраторах.
- **Обновление**: Следите за новыми версиями Spring Boot/Cloud, особенно если используются критические патчи.

---

**При возникновении вопросов**
- Проверяйте логи (консоль или файлы) — там будет информация о маршрутах, ошибках и т.д.
- Открывайте [Spring Cloud Gateway reference](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/) или [OpenAI API docs](https://platform.openai.com/docs/introduction) для более детальной информации о конфигурации и возможностях.

Приятного использования!