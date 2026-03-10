package com.batteryplus.survey.adapter.clientify;

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
                .defaultHeader("Authorization", cfg.getToken())
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

    public ClientifyContact putContact(long contactId, PutContactRequest payload) {
        return web.put()
                .uri("/contacts/{id}/", contactId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ClientifyContact.class)
                .block();
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

    public record PutContactRequest(
            String first_name,
            String last_name,
            String email,
            List<CustomFieldValue> custom_fields
    ) {}

    public record CustomFieldValue(Long field, String value) {}

    public record TagRequest(String name) {}

    public record TagResponse(
            Long id,
            String name
    ) {}
}