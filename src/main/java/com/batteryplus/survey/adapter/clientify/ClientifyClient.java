package com.batteryplus.survey.adapter.clientify;

import com.batteryplus.survey.config.ClientifyConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class ClientifyClient {

    private static final Logger log = LoggerFactory.getLogger(ClientifyClient.class);

    private final WebClient web;
    private final WebClient inlineWeb;
    private final ClientifyConfig cfg;

    public ClientifyClient(ClientifyConfig cfg) {
        this.cfg = cfg;

        this.web = WebClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Authorization", cfg.getToken())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();

        this.inlineWeb = WebClient.builder()
                .baseUrl(cfg.getInlineBaseUrl())
                .defaultHeader("Cookie", cfg.getInlineCookie())
                .defaultHeader("X-CSRFToken", cfg.getInlineCsrfToken())
                .defaultHeader("X-Requested-With", "XMLHttpRequest")
                .defaultHeader("Referer", cfg.getInlineBaseUrl() + "/")
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
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
        return updateInlineField(contactId, "custom_field_" + fieldId, value);
    }

    public String updateInlineField(long contactId, String fieldName, String value) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("name", fieldName);
            form.add("value", value);
            form.add("pk", String.valueOf(contactId));

            return inlineWeb.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(cfg.getInlinePath())
                            .queryParam("value", value)
                            .build())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

        } catch (WebClientResponseException ex) {
            log.error(
                    "Clientify updateInlineField error. contactId={} fieldName={} value={} status={} body={}",
                    contactId,
                    fieldName,
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
            String contact_source,
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
            String contact_source,
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
            String contact_source,
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