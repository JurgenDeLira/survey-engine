package com.batteryplus.survey.adapter.clientify;

import com.batteryplus.survey.config.ClientifyConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class ClientifyClient {

    private static final Logger log = LoggerFactory.getLogger(ClientifyClient.class);

    private final WebClient web;
    private final WebClient webApp;

    public ClientifyClient(ClientifyConfig cfg) {
        this.web = WebClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Authorization", cfg.getToken())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();

        this.webApp = WebClient.builder()
                .baseUrl(cfg.getWebBaseUrl())
                .defaultHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .defaultHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .defaultHeader("Cookie", cfg.getWebCookie())
                .defaultHeader("X-CSRFToken", cfg.getWebCsrfToken())
                .defaultHeader("X-Requested-With", "XMLHttpRequest")
                .defaultHeader("Origin", "https://app.clientify.com")
                .defaultHeader("Referer", "https://app.clientify.com/")
                .build();
    }

    public ClientifyContactSearch searchContacts(String search, int pageSize) {
        return web.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/contacts/")
                        .queryParam("search", search)
                        .queryParam("page_size", pageSize)
                        .build())
                .retrieve()
                .bodyToMono(ClientifyContactSearch.class)
                .block();
    }

    public ClientifyContact createContact(CreateContactRequest payload) {
        try {
            return web.post()
                    .uri("/contacts/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(ClientifyContact.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error(
                    "Clientify createContact error. payload={} status={} body={}",
                    payload,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );
            throw ex;
        }
    }

    public ClientifyContact updateContact(long contactId, UpdateContactRequest payload) {
        try {
            return web.put()
                    .uri("/contacts/{id}/", contactId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(ClientifyContact.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error(
                    "Clientify updateContact error. contactId={} payload={} status={} body={}",
                    contactId,
                    payload,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );
            throw ex;
        }
    }

    public ClientifyContact updateContactDynamic(long contactId, Map<String, Object> payload) {
        try {
            return web.put()
                    .uri("/contacts/{id}/", contactId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(ClientifyContact.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error(
                    "Clientify updateContactDynamic error. contactId={} payload={} status={} body={}",
                    contactId,
                    payload,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );
            throw ex;
        }
    }

    public TagResponse addTagToContact(long contactId, TagRequest payload) {
        return web.post()
                .uri("/contacts/{id}/tags/", contactId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(TagResponse.class)
                .block();
    }

    public String updateCustomFieldInline(long contactId, long fieldId, String value) {
        try {
            return webApp.post()
                    .uri("/object-inline-edit/23/")
                    .body(BodyInserters
                            .fromFormData("name", "custom_field_" + fieldId)
                            .with("value", value)
                            .with("pk", String.valueOf(contactId)))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error(
                    "INLINE EDIT ERROR contactId={} fieldId={} value={} status={} body={}",
                    contactId,
                    fieldId,
                    value,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );
            throw ex;
        }
    }

    public record ClientifyContact(
            Long id,
            String first_name,
            String last_name,
            String email,
            String status,
            List<Phone> phones,
            List<String> tags,
            List<CustomField> custom_fields
    ) {
        public record Phone(Long id, Integer type, String phone, Boolean unsubscribed) {}
        public record CustomField(Long id, String field, String value) {}
    }

    public record ClientifyContactSearch(
            Integer count,
            String next,
            String previous,
            List<Result> results
    ) {
        public record Result(
                Long id,
                String first_name,
                String last_name,
                List<ClientifyContact.Phone> phones
        ) {}
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateContactRequest(
            String first_name,
            String last_name,
            String email,
            String status,
            List<CreatePhone> phones,
            List<CustomFieldValue> custom_fields,
            List<String> tags
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UpdateContactRequest(
            String first_name,
            String last_name,
            String email,
            String status,
            List<CustomFieldValue> custom_fields
    ) {}

    public record CreatePhone(
            Integer type,
            String phone
    ) {}

    public record CustomFieldValue(Long field, String value) {}

    public record TagRequest(String name) {}

    public record TagResponse(
            Long id,
            String name
    ) {}
}