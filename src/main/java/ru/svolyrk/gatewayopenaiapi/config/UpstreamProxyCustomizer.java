package ru.svolyrk.gatewayopenaiapi.config;

import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.stereotype.Component;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

/**
 * Настраивает исходящий HTTP-клиент Gateway на использование upstream-прокси
 * (HTTP или SOCKS5), если в конфиге включено proxy.upstream.enabled.
 */
@Component
public class UpstreamProxyCustomizer implements HttpClientCustomizer {

    private final ProxyProperties proxyProperties;

    public UpstreamProxyCustomizer(ProxyProperties proxyProperties) {
        this.proxyProperties = proxyProperties;
    }

    @Override
    public HttpClient customize(HttpClient httpClient) {
        ProxyProperties.Upstream upstream = proxyProperties.getUpstream();
        if (!upstream.isEnabled() || upstream.getHost() == null || upstream.getHost().isBlank()) {
            return httpClient;
        }

        ProxyProvider.Proxy type = "SOCKS5".equalsIgnoreCase(upstream.getType())
                ? ProxyProvider.Proxy.SOCKS5
                : ProxyProvider.Proxy.HTTP;

        return httpClient.proxy(proxy -> {
            ProxyProvider.Builder builder = proxy.type(type)
                    .host(upstream.getHost())
                    .port(upstream.getPort());
            if (upstream.getUsername() != null && !upstream.getUsername().isBlank()) {
                builder.username(upstream.getUsername());
                if (upstream.getPassword() != null) {
                    builder.password(u -> upstream.getPassword());
                }
            }
        });
    }
}
