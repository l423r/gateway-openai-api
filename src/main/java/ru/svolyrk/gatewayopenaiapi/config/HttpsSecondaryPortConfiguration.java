package ru.svolyrk.gatewayopenaiapi.config;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import javax.net.ssl.KeyManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

/**
 * Запускает второй коннектор — HTTPS на порту 8443 (или из конфига), используя тот же HttpHandler, что и основной сервер.
 * Keystore монтируется в контейнер (например /home/user/gateway-certs/gateway.p12).
 */
@Component
public class HttpsSecondaryPortConfiguration implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(HttpsSecondaryPortConfiguration.class);

    private final ApplicationContext applicationContext;
    private final GatewaySslProperties sslProperties;

    private volatile DisposableServer disposableServer;
    private volatile boolean running;

    public HttpsSecondaryPortConfiguration(ApplicationContext applicationContext,
                                          GatewaySslProperties sslProperties) {
        this.applicationContext = applicationContext;
        this.sslProperties = sslProperties;
    }

    @Override
    public void start() {
        if (!sslProperties.isEnabled()) {
            log.info("[HTTPS] Secondary port disabled (gateway.ssl.enabled=false)");
            return;
        }

        Path keystorePath = Paths.get(sslProperties.getKeystorePath());
        if (!Files.isRegularFile(keystorePath)) {
            log.warn("[HTTPS] Keystore not found at {}, skipping secondary HTTPS port", keystorePath);
            return;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = Files.newInputStream(keystorePath)) {
                keyStore.load(is, sslProperties.getKeystorePassword().toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, sslProperties.getKeystorePassword().toCharArray());

            var sslContext = SslContextBuilder.forServer(kmf)
                    .sslProvider(SslProvider.JDK)
                    .build();

            var httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build();
            var adapter = new ReactorHttpHandlerAdapter(httpHandler);

            HttpServer httpServer = HttpServer.create()
                    .host("0.0.0.0")
                    .port(sslProperties.getPort())
                    .secure(spec -> spec.sslContext(sslContext))
                    .handle(adapter);

            disposableServer = httpServer.bindNow();
            running = true;
            log.info("[HTTPS] Secondary port listening on {}", sslProperties.getPort());
        } catch (Exception e) {
            log.error("[HTTPS] Failed to start secondary port on {}", sslProperties.getPort(), e);
        }
    }

    @Override
    public void stop() {
        if (disposableServer != null) {
            try {
                disposableServer.disposeNow();
            } catch (Exception e) {
                log.warn("[HTTPS] Error disposing secondary server", e);
            }
            disposableServer = null;
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 1;
    }
}
