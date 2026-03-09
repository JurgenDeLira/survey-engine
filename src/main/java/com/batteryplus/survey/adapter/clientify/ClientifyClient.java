package com.batteryplus.survey.adapter.clientify;

// # WebClient calls
import com.batteryplus.survey.config.ClientifyConfig;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class ClientifyClient {

    private final WebClient web;

    public ClientifyClient(ClientifyConfig cfg) {
        this.web = WebClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Authorization", cfg.getToken()) // "Token xxxxx"
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public ClientifyContact getContact(long contactId) {
        return web.get()
                .uri("/contacts/{id}/", contactId)
                .retrieve()
                .bodyToMono(ClientifyContact.class)
                .block();
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
        return web.post()
                .uri("/contacts/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ClientifyContact.class)
                .block();
    }

    /** PATCH /contacts/{id}/ con campos opcionales */
    public ClientifyContact patchContact(long contactId, PatchContactRequest payload) {
        return web.patch()
                .uri("/contacts/{id}/", contactId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ClientifyContact.class)
                .block();
    }

    // -------- DTOs mínimos --------

    public record ClientifyContact(
            Long id,
            String first_name,
            String last_name,
            String email,
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
        public record Result(Long id, List<ClientifyContact.Phone> phones) {}
    }

    public record CreateContactRequest(
            String first_name,
            String last_name,
            String email,
            List<CreatePhone> phones,
            List<String> tags
    ) {}

    public record CreatePhone(
            Integer type,
            String phone
    ) {}

    public record PatchContactRequest(
            List<CustomFieldValue> custom_fields,
            List<String> tags
    ) {}

    public record CustomFieldValue(Long field, String value) {}
}