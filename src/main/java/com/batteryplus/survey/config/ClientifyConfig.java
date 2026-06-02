package com.batteryplus.survey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.clientify")
public class ClientifyConfig {

    private String baseUrl;
    private String token;

    private String inlineBaseUrl;
    private String inlinePath;
    private String inlineCookie;
    private String inlineCsrfToken;

    private String statusClientValue;
    private long contactSourcePisoId;
    private long contactSourceDomicilioId;

    private long ownerMartinId;
    private long ownerDanielaId;

    private CustomFields customFields = new CustomFields();
    private Tags tags = new Tags();

    public static class CustomFields {
        private long fechaUltimaCompraId;
        private long bateriaAdquiridaId;
        private long sucursalId;
        private long anioAutoId;
        private long modeloAutoId;
        private long marcaAutoId;
        private long marcaBateriaId;
        private long gamaId;
        private long fechaFinGarantiaId;

        public long getFechaUltimaCompraId() {
            return fechaUltimaCompraId;
        }

        public void setFechaUltimaCompraId(long fechaUltimaCompraId) {
            this.fechaUltimaCompraId = fechaUltimaCompraId;
        }

        public long getBateriaAdquiridaId() {
            return bateriaAdquiridaId;
        }

        public void setBateriaAdquiridaId(long bateriaAdquiridaId) {
            this.bateriaAdquiridaId = bateriaAdquiridaId;
        }

        public long getSucursalId() {
            return sucursalId;
        }

        public void setSucursalId(long sucursalId) {
            this.sucursalId = sucursalId;
        }

        public long getAnioAutoId() {
            return anioAutoId;
        }

        public void setAnioAutoId(long anioAutoId) {
            this.anioAutoId = anioAutoId;
        }

        public long getModeloAutoId() {
            return modeloAutoId;
        }

        public void setModeloAutoId(long modeloAutoId) {
            this.modeloAutoId = modeloAutoId;
        }

        public long getMarcaAutoId() {
            return marcaAutoId;
        }

        public void setMarcaAutoId(long marcaAutoId) {
            this.marcaAutoId = marcaAutoId;
        }

        public long getMarcaBateriaId() {
            return marcaBateriaId;
        }

        public void setMarcaBateriaId(long marcaBateriaId) {
            this.marcaBateriaId = marcaBateriaId;
        }

        public long getGamaId() {
            return gamaId;
        }

        public void setGamaId(long gamaId) {
            this.gamaId = gamaId;
        }

        public long getFechaFinGarantiaId() {
            return fechaFinGarantiaId;
        }

        public void setFechaFinGarantiaId(long fechaFinGarantiaId) {
            this.fechaFinGarantiaId = fechaFinGarantiaId;
        }
    }

    public static class Tags {
        private String encuestaSatisfaccion;

        public String getEncuestaSatisfaccion() {
            return encuestaSatisfaccion;
        }

        public void setEncuestaSatisfaccion(String encuestaSatisfaccion) {
            this.encuestaSatisfaccion = encuestaSatisfaccion;
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getInlineBaseUrl() {
        return inlineBaseUrl;
    }

    public void setInlineBaseUrl(String inlineBaseUrl) {
        this.inlineBaseUrl = inlineBaseUrl;
    }

    public String getInlinePath() {
        return inlinePath;
    }

    public void setInlinePath(String inlinePath) {
        this.inlinePath = inlinePath;
    }

    public String getInlineCookie() {
        return inlineCookie;
    }

    public void setInlineCookie(String inlineCookie) {
        this.inlineCookie = inlineCookie;
    }

    public String getInlineCsrfToken() {
        return inlineCsrfToken;
    }

    public void setInlineCsrfToken(String inlineCsrfToken) {
        this.inlineCsrfToken = inlineCsrfToken;
    }

    public String getStatusClientValue() {
        return statusClientValue;
    }

    public void setStatusClientValue(String statusClientValue) {
        this.statusClientValue = statusClientValue;
    }

    public long getContactSourcePisoId() {
        return contactSourcePisoId;
    }

    public void setContactSourcePisoId(long contactSourcePisoId) {
        this.contactSourcePisoId = contactSourcePisoId;
    }

    public long getContactSourceDomicilioId() {
        return contactSourceDomicilioId;
    }

    public void setContactSourceDomicilioId(long contactSourceDomicilioId) {
        this.contactSourceDomicilioId = contactSourceDomicilioId;
    }

    public long getOwnerMartinId() {
        return ownerMartinId;
    }

    public void setOwnerMartinId(long ownerMartinId) {
        this.ownerMartinId = ownerMartinId;
    }

    public long getOwnerDanielaId() {
        return ownerDanielaId;
    }

    public void setOwnerDanielaId(long ownerDanielaId) {
        this.ownerDanielaId = ownerDanielaId;
    }

    public CustomFields getCustomFields() {
        return customFields;
    }

    public void setCustomFields(CustomFields customFields) {
        this.customFields = customFields;
    }

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }
}