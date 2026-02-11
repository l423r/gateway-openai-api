# Сертификат: создание в Windows 11 (cmd), установка на Gateway в Docker (Debian 10)

Инструкция целиком: **создать CA и сертификат gateway в Windows 11 через cmd и OpenSSL**, перенести файлы на сервер Debian 10, настроить **Spring Cloud Gateway в Docker** на использование сертификата (HTTPS), плюс установка корневого CA на устройства.

---

## Часть 1. Создание сертификатов в Windows 11 (cmd + OpenSSL)

Откройте **cmd** (Win + R → `cmd` → Enter). Убедитесь, что OpenSSL в PATH: выполните `openssl version`.

### 1.1. Каталог для CA и ключей

```cmd
mkdir %USERPROFILE%\ca
cd %USERPROFILE%\ca
```

Дальше все команды выполняйте из этой папки (например `C:\Users\ВашеИмя\ca`).

### 1.2. Корневой CA (один раз)

```cmd
openssl genrsa -out ca.key 4096
```

```cmd
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 -out ca.crt -subj "/CN=My Gateway Root CA/O=Home/O=Gateway"
```

Если cmd ругается на кавычки или слэши, попробуйте:

```cmd
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 -out ca.crt -subj "//CN=My Gateway Root CA/O=Home/O=Gateway"
```

В папке появятся **ca.key** и **ca.crt**. Файл **ca.crt** потом ставите на телефоны, Windows и телевизор. **ca.key** никуда не копируйте и храните только на этом ПК (или на безопасном носителе).

### 1.3. Сертификат для gateway (ключ и запрос)

```cmd
openssl genrsa -out gateway.key 2048
```

```cmd
openssl req -new -key gateway.key -out gateway.csr -subj "/CN=gateway.local/O=Gateway"
```

### 1.4. Файл расширений (SAN) для gateway

Нужен файл **gateway.ext** в той же папке (`%USERPROFILE%\ca`). В SAN должны быть все домены, которые резолвятся в gateway (те же, что в dnsmasq на роутере), иначе браузер не примет сертификат для этих имён.

**Способ 1 — Блокнот:**

1. Выполните в cmd: `notepad gateway.ext`
2. В Блокноте вставьте (список соответствует доменам из dnsmasq: YouTube, Instagram, Netflix, OpenAI, Mastodon, Telegram):

```ini
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage=digitalSignature,nonRepudiation,keyEncipherment
subjectAltName=@alt_names
[alt_names]
DNS.1=*.youtube.com
DNS.2=youtube.com
DNS.3=*.youtube-nocookie.com
DNS.4=*.googlevideo.com
DNS.5=*.instagram.com
DNS.6=*.netflix.com
DNS.7=*.nflxvideo.net
DNS.8=api.openai.com
DNS.9=*.openai.com
DNS.10=*.mastodon.social
DNS.11=*.ytimg.com
DNS.12=*.ggpht.com
DNS.13=*.gstatic.com
DNS.14=*.googleapis.com
DNS.15=*.telegram.org
DNS.16=telegram.org
DNS.17=*.t.me
DNS.18=t.me
```

3. Сохраните файл (кодировка UTF-8 или ANSI), закройте Блокнот.

**Способ 2 — через echo (по одной строке в cmd):**

```cmd
echo authorityKeyIdentifier=keyid,issuer> gateway.ext
echo basicConstraints=CA:FALSE>> gateway.ext
echo keyUsage=digitalSignature,nonRepudiation,keyEncipherment>> gateway.ext
echo subjectAltName=@alt_names>> gateway.ext
echo [alt_names]>> gateway.ext
echo DNS.1=*.youtube.com>> gateway.ext
echo DNS.2=youtube.com>> gateway.ext
echo DNS.3=*.youtube-nocookie.com>> gateway.ext
echo DNS.4=*.googlevideo.com>> gateway.ext
echo DNS.5=*.instagram.com>> gateway.ext
echo DNS.6=*.netflix.com>> gateway.ext
echo DNS.7=*.nflxvideo.net>> gateway.ext
echo DNS.8=api.openai.com>> gateway.ext
echo DNS.9=*.openai.com>> gateway.ext
echo DNS.10=*.mastodon.social>> gateway.ext
echo DNS.11=*.ytimg.com>> gateway.ext
echo DNS.12=*.ggpht.com>> gateway.ext
echo DNS.13=*.gstatic.com>> gateway.ext
echo DNS.14=*.googleapis.com>> gateway.ext
echo DNS.15=*.telegram.org>> gateway.ext
echo DNS.16=telegram.org>> gateway.ext
echo DNS.17=*.t.me>> gateway.ext
echo DNS.18=t.me>> gateway.ext
```

Если позже добавите домены в dnsmasq — добавьте для них строки DNS.N=... в gateway.ext и перевыпустите сертификат (шаги 1.5 и 1.6).

### 1.5. Подпись сертификата gateway вашим CA

```cmd
openssl x509 -req -in gateway.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out gateway.crt -days 825 -sha256 -extfile gateway.ext
```

В папке появятся **gateway.crt**, **gateway.key** и (после первой подписи) **ca.srl**.

### 1.6. Сборка PKCS12 (keystore) для Spring Boot

Spring Boot удобно настраивать на один файл keystore в формате PKCS12. В той же папке выполните:

```cmd
openssl pkcs12 -export -out gateway.p12 -inkey gateway.key -in gateway.crt -certfile ca.crt -passout pass:changeit
```

Пароль `changeit` замените на свой и запомните — он понадобится в конфиге gateway. Имя файла **gateway.p12** можно оставить или изменить.

**Итог в папке `%USERPROFILE%\ca`:**

| Файл        | Назначение |
|------------|------------|
| ca.key     | Секрет CA — не копировать на сервер и устройства |
| ca.crt     | Корневой сертификат — установить на Android, Windows 11, телевизор |
| gateway.key| Секрет gateway — можно не копировать на сервер, если используете только .p12 |
| gateway.crt| Сертификат gateway — уже внутри gateway.p12 |
| gateway.p12| Keystore для gateway (сертификат + ключ + CA) — копировать на Debian и использовать в Docker |

На сервер Debian нужны минимум: **gateway.p12** и (для установки на устройства) **ca.crt**. Файлы **ca.key** и **gateway.key** на сервер лучше не класть.

---

## Часть 2. Копирование на сервер Debian 10

С Windows на Debian можно перенести файлы так:

**Вариант A — scp из cmd (если установлен OpenSSH):**

```cmd
scp %USERPROFILE%\ca\gateway.p12 %USERPROFILE%\ca\ca.crt user@88.210.20.137:/home/user/gateway-certs/
```

Подставьте логин и путь. Каталог на сервере создайте заранее: `mkdir -p /home/user/gateway-certs`.

**Вариант B — WinSCP / FileZilla:** подключитесь к 88.210.20.137 по SFTP и загрузите в выбранную папку (например `/home/user/gateway-certs/`) файлы **gateway.p12** и **ca.crt**.

**На Debian** проверьте права (чтобы контейнер мог читать):

```bash
sudo chown -R root:root /home/user/gateway-certs
chmod 600 /home/user/gateway-certs/gateway.p12
chmod 644 /home/user/gateway-certs/ca.crt
```

Путь можно заменить на свой, например `/opt/gateway-certs/`.

---

## Часть 3. Gateway в Docker на Debian 10 с сертификатом

Gateway должен слушать HTTPS (порт 443) и использовать **gateway.p12**. Делается это за счёт настроек Spring Boot и монтирования файла в контейнер.

### 3.1. Структура на сервере

На сервере Debian каталог с сертификатами: **/home/user/gateway-certs/** с файлами **gateway.p12** и **ca.crt**. Приложение поднимает два порта: **8080** (HTTP) и **8443** (HTTPS, второй коннектор с сертификатом из gateway.p12).

### 3.2. Запуск контейнера с монтированием сертификата

Запуск с привязкой портов 8080 (HTTP) и 443→8443 (HTTPS) и монтированием каталога с сертификатами:

```bash
docker run -d \
  --name gateway-openai-api \
  -p 8080:8080 \
  -p 443:8443 \
  -v /home/user/gateway-certs:/home/user/gateway-certs:ro \
  gateway-openai-api:1.0
```

Пароль keystore по умолчанию `changeit` (задаётся в application.yml). Чтобы задать свой пароль: `-e GATEWAY_SSL_KEYSTORE_PASSWORD=ваш_пароль`.

**В проекте реализованы оба порта:** HTTP 8080 и HTTPS 8443 (второй коннектор поднимается автоматически, если в контейнере есть файл `gateway.p12` по пути из `gateway.ssl.keystore-path`). Используйте маппинг `-p 8080:8080` и `-p 443:8443`, том `/home/user/gateway-certs:/home/user/gateway-certs:ro` — как в CI/CD (см. ниже).

### 3.3. Конфигурация в приложении

В **application.yml** уже заданы настройки второго коннектора (HTTPS 8443):

- `gateway.ssl.enabled` — включить/выключить второй порт (по умолчанию `true`).
- `gateway.ssl.port` — порт HTTPS (8443).
- `gateway.ssl.keystore-path` — путь к **gateway.p12** внутри контейнера: `/home/user/gateway-certs/gateway.p12`.
- `gateway.ssl.keystore-password` — пароль (по умолчанию `changeit`, переопределяется переменной `GATEWAY_SSL_KEYSTORE_PASSWORD`).

Сертификаты в образ не входят, они монтируются с хоста при запуске контейнера.

### 3.4. Проверка

После запуска контейнера с хоста Debian:

```bash
curl -k -v https://localhost:8443
```

или с другого ПК (подставьте IP сервера):

```bash
curl -k -v https://88.210.20.137:443
```

Флаг `-k` временно отключает проверку сертификата. В ответе должен быть ваш gateway (ошибка 404 или маршрут — в зависимости от пути). В браузере без установки **ca.crt** будет предупреждение о недоверенном сертификате — это нормально до установки корневого CA на устройства.

---

## Часть 4. Установка корневого сертификата (ca.crt) на устройствах

Чтобы браузеры и приложения доверяли вашему gateway, на каждом устройстве нужно установить **ca.crt** как доверенный корневой CA.

- **Android (телефон/планшет):** Настройки → Безопасность → Шифрование и учётные данные → Установить сертификат → CA-сертификат → выбрать **ca.crt**. Подробнее: [option2-tls-termination.md](option2-tls-termination.md).
- **Windows 11:** `Win + R` → `certmgr.msc` → Доверенные корневые центры сертификации → Сертификаты → ПКМ → Все задачи → Импорт → указать **ca.crt**. Подробнее: [option2-tls-termination.md](option2-tls-termination.md).
- **Телевизор на Android:** через настройки (если есть пункт про сертификаты) или через adb; см. [option2-tls-termination.md](option2-tls-termination.md).

Файл **ca.crt** можно раздать по почте, облаку или скопировать с того же ПК, где создавали сертификаты.

---

## Краткая последовательность

1. **Windows 11 (cmd):** создать в `%USERPROFILE%\ca` CA (ca.key, ca.crt), сертификат gateway (gateway.key, gateway.crt, gateway.ext), собрать **gateway.p12**.
2. **Копирование:** перенести **gateway.p12** и **ca.crt** на Debian (например в `/opt/gateway-certs/`).
3. **Docker на Debian:** запускать контейнер с `-v .../gateway.p12:/opt/gateway-certs/gateway.p12:ro` и переменными (или конфигом) для SSL; пробросить порт 443 на 8443 (или на тот порт, на котором в приложении включён HTTPS).
4. **Устройства:** установить **ca.crt** как доверенный корневой CA (см. ссылку выше).

После этого HTTPS к доменам, для которых DNS отдаёт IP gateway (см. [dnsmasq-only-selected-hosts.md](dnsmasq-only-selected-hosts.md)), будет обслуживаться вашим gateway с вашим сертификатом без предупреждений в браузере.

---

## Если в логах шлюза ничего не видно при открытии ссылки

Если запрос не попадает в логи — он не доходит до приложения. Пошаговая диагностика: **[transparent-proxy-troubleshooting.md](transparent-proxy-troubleshooting.md)** (DNS на клиенте, curl до шлюза, DNAT на роутере, порт 8443).
