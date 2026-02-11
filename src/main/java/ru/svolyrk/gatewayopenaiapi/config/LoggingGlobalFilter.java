package ru.svolyrk.gatewayopenaiapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Глобальный фильтр, логирует входящие запросы и исходящие ответы.
 */
@Configuration
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        // Логируем информацию о запросе (Host нужен для диагностики прозрачного прокси)
        String host = exchange.getRequest().getHeaders().getFirst("Host");
        String requestPath = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        log.info("[GATEWAY] Incoming request: {} {} Host: {}", method, requestPath, host != null ? host : "-");

        // Продолжаем фильтрацию
        return chain.filter(exchange).then(
            Mono.fromRunnable(() -> {
                // Когда ответ сформировался, логируем статус
                int statusCode = 0;
                statusCode = exchange.getResponse().getStatusCode().value();
                log.info("[GATEWAY] Response status code: {}", statusCode);
            })
        );
    }

    @Override
    public int getOrder() {
        // Указываем порядок, чтобы фильтр сработал в начале (LOWEST_PRECEDENCE => позже всех, HIGHEST_PRECEDENCE => раньше всех)
        return -1;
    }
}
