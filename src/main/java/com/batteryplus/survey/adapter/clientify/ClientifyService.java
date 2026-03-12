package com.batteryplus.survey.adapter.clientify;

import com.batteryplus.survey.config.ClientifyConfig;
import com.batteryplus.survey.core.model.VerinaTicketRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Service
public class ClientifyService {

    private static final Logger log = LoggerFactory.getLogger(ClientifyService.class);
    private static final Integer PHONE_TYPE_MOBILE = 1;
    private static final String STATUS_VENTA = "venta";
    private static final int MAX_RETRIES = 3;

    private final ClientifyClient client;
    private final ClientifyConfig cfg;

    public ClientifyService(ClientifyClient client, ClientifyConfig cfg) {
        this.client = client;
        this.cfg = cfg;
    }

    public boolean upsertContactFromSale(String phoneE164, String ticketValue, VerinaTicketRow row) {
        if (phoneE164 == null || phoneE164.isBlank()) return false;
        if (ticketValue == null || ticketValue.isBlank()) return false;
        if (row == null) return false;

        String safeEmail = sanitizeEmail(row.correoElectronico());
        Long contactId = findExactContactIdByPhone(phoneE164);

        long fieldId = cfg.getCustomFields().getUltimaCompraTicketId();

        if (contactId != null) {
            log.info("Contacto encontrado en Clientify. contactId={} phone={}", contactId, phoneE164);

            var payload = new ClientifyClient.UpdateContactRequest(
                    cleanName(row.nombre()),
                    cleanLastName(row.apellido()),
                    safeEmail,
                    STATUS_VENTA,
                    List.of(new ClientifyClient.CustomFieldValue(fieldId, ticketValue))
            );

            log.info(
                    "Clientify PUT -> contactId={} fieldId={} ticketValue={} status={}",
                    contactId, fieldId, ticketValue, STATUS_VENTA
            );

            var response = executeWithRetry(() -> client.updateContact(contactId, payload));

            log.info("Clientify PUT response -> contactId={} response={}", contactId, response);

            tryAddTag(contactId);

            if (response != null && (response.custom_fields() == null || response.custom_fields().isEmpty())) {
                log.warn(
                        "Clientify aceptó la actualización pero no reflejó custom_fields. contactId={} fieldId={} ticketValue={}",
                        contactId, fieldId, ticketValue
                );
            }

            return true;
        }

        var createPayload = new ClientifyClient.CreateContactRequest(
                cleanName(row.nombre()),
                cleanLastName(row.apellido()),
                safeEmail,
                STATUS_VENTA,
                List.of(new ClientifyClient.CreatePhone(PHONE_TYPE_MOBILE, phoneE164)),
                List.of(new ClientifyClient.CustomFieldValue(fieldId, ticketValue)),
                List.of(encuestaTag())
        );

        log.info(
                "Clientify create -> firstName={} lastName={} email={} phone={} status={} tag={}",
                cleanName(row.nombre()), cleanLastName(row.apellido()), safeEmail, phoneE164, STATUS_VENTA, encuestaTag()
        );

        var created = executeWithRetry(() -> client.createContact(createPayload));

        log.info("Clientify create response -> response={}", created);

        if (created == null || created.id() == null) {
            log.warn("No se pudo crear contacto en Clientify. phone={} ticketValue={}", phoneE164, ticketValue);
            return false;
        }

        tryAddTag(created.id());

        if (created.custom_fields() == null || created.custom_fields().isEmpty()) {
            log.warn(
                    "Clientify creó/actualizó contacto pero no reflejó custom_fields. contactId={} fieldId={} ticketValue={}",
                    created.id(), fieldId, ticketValue
            );
        }

        return true;
    }

    private Long findExactContactIdByPhone(String phoneE164) {
        var search = client.searchContacts(phoneE164, 20);
        if (search == null || search.results() == null) return null;

        String normalizedTarget = normalizePhone(phoneE164);
        Long found = null;

        for (var result : search.results()) {
            if (result == null || result.phones() == null) continue;

            for (var phone : result.phones()) {
                if (phone == null || phone.phone() == null) continue;

                if (normalizedTarget.equals(normalizePhone(phone.phone()))) {
                    if (found != null && !found.equals(result.id())) {
                        log.warn("Más de un contacto coincide con el mismo teléfono exacto. phone={}", phoneE164);
                        return null;
                    }
                    found = result.id();
                }
            }
        }

        return found;
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

    private String normalizePhone(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^0-9+]", "");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private <T> T executeWithRetry(Supplier<T> action) {
        RuntimeException last = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.get();
            } catch (WebClientResponseException ex) {
                // 4xx = payload/negocio, no reintentar
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
}