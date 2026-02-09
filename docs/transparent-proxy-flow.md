# Поток запросов: прозрачный прокси через Spring Cloud Gateway

Диаграмма описывает сценарий, когда роутер (ASUS RT-AX53U) перенаправляет трафик на Spring Cloud Gateway (DNAT), а gateway проксирует запросы на целевые сайты, опционально через внешний HTTP/SOCKS-прокси.

**Участники:**
- **DeviceInLAN** — устройство в локальной сети (ПК, смартфон).
- **AsusRouter** — роутер ASUS RT-AX53U (правила iptables DNAT).
- **SpringGateway** — Spring Cloud Gateway (например, `192.168.1.100:8080`).
- **ExternalProxy** — опционально: внешний прокси/VPN за рубежом.
- **TargetSites** — целевые сайты (YouTube, OpenAI, Netflix, Mastodon и т.д.).

```mermaid
sequenceDiagram
    participant Client as DeviceInLAN
    participant Router as AsusRouter
    participant Gateway as SpringGateway
    participant Upstream as ExternalProxy
    participant Target as TargetSites

    Client->>Router: HTTP Host www.youtube.com
    Router->>Gateway: DNAT same request to port 8080
    Gateway->>Gateway: Route by Host, dynamic URI
    alt Upstream proxy configured
        Gateway->>Upstream: Request via proxy
        Upstream->>Target: Request to target site
        Target-->>Upstream: Response
        Upstream-->>Gateway: Response
    else Direct connection
        Gateway->>Target: Direct request to target
        Target-->>Gateway: Response
    end
    Gateway-->>Router: Response
    Router-->>Client: Response
```
