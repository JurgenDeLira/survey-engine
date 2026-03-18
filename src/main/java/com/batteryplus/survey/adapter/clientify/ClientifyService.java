package com.batteryplus.survey.adapter.clientify;

import com.batteryplus.survey.config.ClientifyConfig;
import com.batteryplus.survey.core.model.VerinaTicketRow;
import com.batteryplus.survey.core.normalize.PhoneNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class ClientifyService {

    private static final Logger log = LoggerFactory.getLogger(ClientifyService.class);
    private static final Integer PHONE_TYPE_MOBILE = 1;
    private static final String STATUS_VENTA = "venta";
    private static final int MAX_RETRIES = 3;
    private static final int SEARCH_PAGE_SIZE = 50;

    private final ClientifyClient client;
    private final ClientifyConfig cfg;
    private final ObjectMapper objectMapper;
    private final PhoneNormalizer phoneNormalizer;

    public ClientifyService(
            ClientifyClient client,
            ClientifyConfig cfg,
            ObjectMapper objectMapper,
            PhoneNormalizer phoneNormalizer
    ) {
        this.client = client;
        this.cfg = cfg;
        this.objectMapper = objectMapper;
        this.phoneNormalizer = phoneNormalizer;
    }

    public boolean upsertContactFromSale(String phoneE164, String ticketValue, VerinaTicketRow row) {
        log.warn("VERSION NUEVA CLIENTIFYSERVICE ACTIVA");

        String normalizedPhone = phoneNormalizer.toE164OrNull(phoneE164);

        if (normalizedPhone == null || normalizedPhone.isBlank()) return false;
        if (ticketValue == null || ticketValue.isBlank()) return false;
        if (row == null) return false;

        String safeEmail = sanitizeEmail(row.correoElectronico());
        Long existingContactId = findExistingContactIdByPhone(normalizedPhone);

        if (existingContactId != null) {
            log.info("Contacto encontrado en Clientify. contactId={} phone={}", existingContactId, normalizedPhone);

            var updatePayload = new ClientifyClient.UpdateContactRequest(
                    cleanName(row.nombre()),
                    cleanLastName(row.apellido()),
                    safeEmail,
                    STATUS_VENTA,
                    null
            );

            try {
                log.info("Clientify update payload JSON={}", objectMapper.writeValueAsString(updatePayload));
            } catch (Exception e) {
                log.warn("No se pudo serializar payload de update", e);
            }

            var updated = executeWithRetry(() -> client.updateContact(existingContactId, updatePayload));

            log.info("Clientify PUT response -> contactId={} response={}", existingContactId, updated);

            tryAddTag(existingContactId);

            if (updated == null || updated.id() == null) {
                log.warn("Clientify no devolvió contacto actualizado. contactId={} ticketValue={}", existingContactId, ticketValue);
                return false;
            }

            if (!containsTicketFieldValue(updated.custom_fields(), ticketValue)) {
                log.warn(
                        "Clientify aceptó la actualización pero no reflejó el campo ticket. contactId={} ticketValue={}",
                        existingContactId, ticketValue
                );
            }

            return true;
        }

        List<ClientifyClient.CustomFieldValue> customFields = buildCustomFields(ticketValue, row);

        var createPayload = new ClientifyClient.CreateContactRequest(
                cleanName(row.nombre()),
                cleanLastName(row.apellido()),
                safeEmail,
                STATUS_VENTA,
                List.of(new ClientifyClient.CreatePhone(PHONE_TYPE_MOBILE, normalizedPhone)),
                customFields,
                List.of(encuestaTag())
        );

        try {
            log.info("Clientify create payload JSON={}", objectMapper.writeValueAsString(createPayload));
        } catch (Exception e) {
            log.warn("No se pudo serializar payload de create", e);
        }

        var created = executeWithRetry(() -> client.createContact(createPayload));

        log.info("Clientify create response -> response={}", created);

        if (created == null || created.id() == null) {
            log.warn("No se pudo crear contacto en Clientify. phone={} ticketValue={}", normalizedPhone, ticketValue);
            return false;
        }

        tryAddTag(created.id());

        if (!containsTicketFieldValue(created.custom_fields(), ticketValue)) {
            log.warn(
                    "Clientify creó contacto pero no reflejó el campo ticket. contactId={} ticketValue={}",
                    created.id(), ticketValue
            );
        }

        return true;
    }

    private Long findExistingContactIdByPhone(String phoneE164) {
        SearchVariants variants = buildSearchVariants(phoneE164);

        log.info(
                "Clientify search variants -> input={} plus52={} plus521={} local10={} comparable={}",
                phoneE164,
                variants.plus52(),
                variants.plus521(),
                variants.local10(),
                variants.comparable()
        );

        Set<String> queries = new LinkedHashSet<>();
        if (variants.plus52() != null) queries.add(variants.plus52());
        if (variants.plus521() != null) queries.add(variants.plus521());
        if (variants.local10() != null) queries.add(variants.local10());

        for (String query : queries) {
            ClientifyClient.ClientifyContactSearch response;
            try {
                response = executeWithRetry(() -> client.searchContacts(query, SEARCH_PAGE_SIZE));
            } catch (Exception ex) {
                log.warn("Falló búsqueda Clientify. query={}", query, ex);
                continue;
            }

            int results = response == null || response.results() == null ? 0 : response.results().size();
            log.info("Clientify search query='{}' results={}", query, results);

            if (response == null || response.results() == null) {
                continue;
            }

            for (ClientifyClient.ClientifyContactSearch.Result result : response.results()) {
                if (result == null || result.phones() == null) {
                    continue;
                }

                for (ClientifyClient.ClientifyContact.Phone phone : result.phones()) {
                    String comparable = comparablePhone(phone.phone());

                    log.info(
                            "Clientify candidate -> query='{}' contactId={} phone={} comparable={}",
                            query,
                            result.id(),
                            phone.phone(),
                            comparable
                    );

                    if (variants.comparable().equals(comparable)) {
                        return result.id();
                    }
                }
            }
        }

        return null;
    }

    private List<ClientifyClient.CustomFieldValue> buildCustomFields(String ticketValue, VerinaTicketRow row) {
        var f = cfg.getCustomFields();
        List<ClientifyClient.CustomFieldValue> fields = new ArrayList<>();

        String valueToSend = row.meUltimaCompraTicket();
        if (valueToSend == null || valueToSend.isBlank()) {
            valueToSend = ticketValue;
        }

        addField(fields, f.getUltimaCompraTicketId(), valueToSend);
        return fields;
    }

    private void addField(List<ClientifyClient.CustomFieldValue> fields, long fieldId, String value) {
        if (fieldId <= 0) return;
        if (value == null) return;

        String clean = value.trim();
        if (clean.isBlank() || clean.equals("-")) return;

        fields.add(new ClientifyClient.CustomFieldValue(fieldId, clean));
    }

    private boolean containsTicketFieldValue(List<ClientifyClient.ClientifyContact.CustomField> customFields, String expectedValue) {
        if (customFields == null || customFields.isEmpty()) return false;
        if (expectedValue == null || expectedValue.isBlank()) return false;

        String expectedFieldName = "me_ultima compra ticket";

        for (ClientifyClient.ClientifyContact.CustomField field : customFields) {
            if (field == null) continue;

            String apiFieldName = field.field() == null ? "" : field.field().trim().toLowerCase(Locale.ROOT);
            String apiValue = field.value() == null ? "" : field.value().trim();

            if (expectedFieldName.equals(apiFieldName) && expectedValue.trim().equals(apiValue)) {
                return true;
            }
        }

        return false;
    }

    private void tryAddTag(Long contactId) {
        try {
            var tagResponse = client.addTagToContact(
                    contactId,
                    new ClientifyClient.TagRequest(encuestaTag())
            );
            log.info("Clientify tag response -> contactId={} response={}", contactId, tagResponse);
        } catch (Exception ex) {
            log.warn("Clientify no pudo agregar tag. contactId={} tag={}", contactId, encuestaTag(), ex);
        }
    }

    private String encuestaTag() {
        String tag = cfg.getTags().getEncuestaSatisfaccion();
        return (tag == null || tag.isBlank()) ? "prueba_jorge" : tag.trim();
    }

    private String sanitizeEmail(String email) {
        String safe = safeNullable(email);
        if (safe == null) return null;

        String lower = safe.toLowerCase(Locale.ROOT);

        if (lower.contains("servicioadomicilio")
                || lower.contains("facturacion")
                || lower.contains("ventas")
                || lower.contains("servdom")
                || lower.contains("servicio.domicilio")
                || lower.endsWith("@batteryplus.mx")
                || lower.equals("aa@gmail.com")) {
            return null;
        }

        return safe;
    }

    private String cleanName(String value) {
        String s = safe(value);
        if (s.isBlank() || s.equals("-")) return "SIN NOMBRE";
        return s;
    }

    private String cleanLastName(String value) {
        String s = safe(value);
        if (s.isBlank() || s.equals("-")) return "SIN APELLIDO";
        return s;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private SearchVariants buildSearchVariants(String phoneE164) {
        String digits = onlyDigits(phoneE164);
        String local10 = null;

        if (digits.length() >= 10) {
            local10 = digits.substring(digits.length() - 10);
        }

        String plus52 = local10 == null ? null : "+52" + local10;
        String plus521 = local10 == null ? null : "+521" + local10;
        String comparable = plus52 == null ? null : comparablePhone(plus52);

        return new SearchVariants(plus52, plus521, local10, comparable);
    }

    private String comparablePhone(String raw) {
        String digits = onlyDigits(raw);
        if (digits.isBlank()) return "";

        if (digits.startsWith("521") && digits.length() >= 13) {
            return "52" + digits.substring(3);
        }

        if (digits.startsWith("52") && digits.length() >= 12) {
            return digits;
        }

        if (digits.length() == 10) {
            return "52" + digits;
        }

        if (digits.length() > 12) {
            return digits.substring(digits.length() - 12);
        }

        return digits;
    }

    private String onlyDigits(String value) {
        if (value == null) return "";
        return value.replaceAll("\\D", "");
    }

    private <T> T executeWithRetry(Supplier<T> action) {
        RuntimeException last = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.get();
            } catch (WebClientResponseException ex) {
                if (ex.getStatusCode().is4xxClientError()) {
                    throw ex;
                }

                last = ex;
                if (attempt < MAX_RETRIES) {
                    log.warn("Clientify retry {}/{}", attempt, MAX_RETRIES);
                    sleep(700L * attempt);
                }
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt < MAX_RETRIES) {
                    log.warn("Clientify retry {}/{}", attempt, MAX_RETRIES);
                    sleep(700L * attempt);
                }
            }
        }

        throw last;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private record SearchVariants(
            String plus52,
            String plus521,
            String local10,
            String comparable
    ) {}
}