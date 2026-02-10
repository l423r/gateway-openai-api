package ru.svolyrk.gatewayopenaiapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурация дополнительного HTTPS-порта (второй коннектор рядом с HTTP 8080).
 */
@Component
@ConfigurationProperties(prefix = "gateway.ssl")
public class GatewaySslProperties {

    /** Включить ли второй порт с HTTPS. */
    private boolean enabled = true;

    /** Порт HTTPS (по умолчанию 8443). */
    private int port = 8443;

    /** Путь к keystore (PKCS12) внутри контейнера/на сервере. */
    private String keystorePath = "/home/user/gateway-certs/gateway.p12";

    /** Пароль keystore. */
    private String keystorePassword = "changeit";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath != null ? keystorePath : "";
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword != null ? keystorePassword : "";
    }
}
