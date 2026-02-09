package ru.svolyrk.gatewayopenaiapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Конфигурация прозрачного прокси и опционального upstream-прокси.
 */
@Component
@ConfigurationProperties(prefix = "proxy")
public class ProxyProperties {

    /**
     * Список доменов/паттернов для прозрачного прокси (Ant-style, через запятую в yaml или как список).
     * Используется в предикате Host и в TransparentProxyFilter.
     */
    private String domains = "";

    private final Upstream upstream = new Upstream();

    public String getDomains() {
        return domains;
    }

    public void setDomains(String domains) {
        this.domains = domains != null ? domains : "";
    }

    /**
     * Возвращает список доменов для проверки (разбиение по запятой, trim).
     */
    public List<String> getDomainsList() {
        List<String> result = new ArrayList<>();
        if (domains == null || domains.isBlank()) {
            return result;
        }
        for (String s : domains.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                result.add(t);
            }
        }
        return result;
    }

    public Upstream getUpstream() {
        return upstream;
    }

    public static class Upstream {
        private boolean enabled = false;
        private String type = "HTTP";
        private String host = "";
        private int port = 3128;
        private String username;
        private String password;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type != null ? type : "HTTP";
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host != null ? host : "";
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
