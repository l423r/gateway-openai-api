package ru.svolyrk.gatewayopenaiapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * Глобальный фильтр для прозрачного прокси: подменяет целевой URL на тот,
 * что пришёл в запросе (Host + path + query), чтобы трафик с роутера (DNAT)
 * перенаправлялся на реальный хост. Работает только для маршрута transparent-proxy-route.
 */
@Component
public class TransparentProxyFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TransparentProxyFilter.class);
    private static final String TRANSPARENT_PROXY_ROUTE_ID = "transparent-proxy-route";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null || !TRANSPARENT_PROXY_ROUTE_ID.equals(route.getId())) {
            return chain.filter(exchange);
        }

        String host = exchange.getRequest().getHeaders().getFirst("Host");
        if (host == null || host.isBlank()) {
            log.warn("[GATEWAY] Transparent proxy: missing Host header");
            return chain.filter(exchange);
        }

        // Убираем порт из Host если есть (для целевого URI используем стандартные порты)
        String hostOnly = host.contains(":") ? host.substring(0, host.indexOf(':')) : host;
        String path = exchange.getRequest().getURI().getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String query = exchange.getRequest().getURI().getRawQuery();
        String scheme = "https";

        String targetUriStr = scheme + "://" + hostOnly + path + (query != null && !query.isEmpty() ? "?" + query : "");
        URI targetUri = URI.create(targetUriStr);
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, targetUri);

        if (log.isDebugEnabled()) {
            log.debug("[GATEWAY] Transparent proxy target: {}", targetUriStr);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return ROUTE_TO_URL_FILTER_ORDER + 1;
    }
}
