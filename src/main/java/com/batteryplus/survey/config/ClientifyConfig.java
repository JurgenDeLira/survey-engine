package com.batteryplus.survey.config;

//WebClient + auth
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.clientify")
public class ClientifyConfig {

    private String baseUrl;
    private String token;
    private CustomFields customFields = new CustomFields();
    private Tags tags = new Tags();

    public static class CustomFields {
        private long ultimaCompraTicketId;

        public long getUltimaCompraTicketId() { return ultimaCompraTicketId; }
        public void setUltimaCompraTicketId(long ultimaCompraTicketId) { this.ultimaCompraTicketId = ultimaCompraTicketId; }
    }

    public static class Tags {
        private String encuestaSatisfaccion;

        public String getEncuestaSatisfaccion() { return encuestaSatisfaccion; }
        public void setEncuestaSatisfaccion(String encuestaSatisfaccion) { this.encuestaSatisfaccion = encuestaSatisfaccion; }
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public CustomFields getCustomFields() { return customFields; }
    public void setCustomFields(CustomFields customFields) { this.customFields = customFields; }

    public Tags getTags() { return tags; }
    public void setTags(Tags tags) { this.tags = tags; }
}