package com.batteryplus.survey.adapter.clientify;

import com.batteryplus.survey.config.ClientifyConfig;
import com.batteryplus.survey.core.model.VerinaTicketRow;
import com.batteryplus.survey.core.normalize.PhoneNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.format.DateTimeFormatter;
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
    private static final int MAX_RETRIES = 3;
    private static final int SEARCH_PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

    public boolean upsertContactFromSale(String phoneE164, String ignoredTicketValue, VerinaTicketRow row) {
        return upsertContactFromSaleDetailed(phoneE164, ignoredTicketValue, row).ok();
    }

    public UpsertResult upsertContactFromSaleDetailed(String phoneE164, String ignoredTicketValue, VerinaTicketRow row) {
        log.warn("VERSION NUEVA CLIENTIFYSERVICE ACTIVA");

        String normalizedPhone = phoneNormalizer.toE164OrNull(phoneE164);

        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            return new UpsertResult(false, null, false, "Telefono inválido/no normalizable", false);
        }

        if (row == null) {
            return new UpsertResult(false, null, false, "row nulo", false);
        }

        String safeEmail = sanitizeEmail(row.correoElectronico());
        Long existingContactId = findExistingContactIdByPhone(normalizedPhone);

        if (existingContactId != null) {
            log.info("Contacto encontrado en Clientify. contactId={} phone={}", existingContactId, normalizedPhone);

            var updatePayload = new ClientifyClient.UpdateContactRequest(
                    cleanName(row.nombre()),
                    cleanLastName(row.apellido()),
                    safeEmail,
                    null,
                    null,
                    null
            );

            try {
                log.info("Clientify update payload JSON={}", objectMapper.writeValueAsString(updatePayload));
            } catch (Exception e) {
                log.warn("No se pudo serializar payload de update", e);
            }

            try {
                var updated = executeWithRetry(() -> client.updateContact(existingContactId, updatePayload));

                log.info("Clientify PUT response -> contactId={} response={}", existingContactId, updated);

                if (updated == null || updated.id() == null) {
                    log.warn("Clientify no devolvió contacto actualizado. contactId={}", existingContactId);
                    return new UpsertResult(false, existingContactId, false, "PUT sin respuesta válida", false);
                }

                InlineSyncResult inline = syncInlineForExistingContact(existingContactId, row);

                return new UpsertResult(true, existingContactId, inline.success(), inline.errorMessage(), false);
            } catch (Exception ex) {
                log.error("Error en update de Clientify. contactId={}", existingContactId, ex);
                return new UpsertResult(false, existingContactId, false, ex.getMessage(), false);
            }
        }

        var createPayload = new ClientifyClient.CreateContactRequest(
                cleanName(row.nombre()),
                cleanLastName(row.apellido()),
                safeEmail,
                null,
                null,
                List.of(new ClientifyClient.CreatePhone(PHONE_TYPE_MOBILE, normalizedPhone)),
                null,
                null
        );

        try {
            log.info("Clientify create payload JSON={}", objectMapper.writeValueAsString(createPayload));
        } catch (Exception e) {
            log.warn("No se pudo serializar payload de create", e);
        }

        try {
            var created = executeWithRetry(() -> client.createContact(createPayload));

            log.info("Clientify create response -> response={}", created);

            if (created == null || created.id() == null) {
                log.warn("No se pudo crear contacto en Clientify. phone={}", normalizedPhone);
                return new UpsertResult(false, null, false, "POST sin respuesta válida", false);
            }

            Long contactId = created.id();

            var secondStepUpdate = new ClientifyClient.UpdateContactRequest(
                    cleanName(row.nombre()),
                    cleanLastName(row.apellido()),
                    safeEmail,
                    null,
                    null,
                    null
            );

            try {
                log.info("Clientify second-step update payload JSON={}", objectMapper.writeValueAsString(secondStepUpdate));
            } catch (Exception e) {
                log.warn("No se pudo serializar payload de second-step update", e);
            }

            var updatedAfterCreate = executeWithRetry(() -> client.updateContact(contactId, secondStepUpdate));

            log.info("Clientify second-step PUT response -> contactId={} response={}", contactId, updatedAfterCreate);

            if (updatedAfterCreate == null || updatedAfterCreate.id() == null) {
                log.warn("Clientify creó contacto pero no devolvió respuesta válida en segundo paso. contactId={}", contactId);
                return new UpsertResult(false, contactId, false, "Second PUT sin respuesta válida", false);
            }

            boolean reallyNewContact = !looksLikePreexistingContact(created);

            if (!reallyNewContact) {
                log.info(
                        "POST devolvió contacto con historial previo. Se tratará como existente. contactId={}",
                        contactId
                );
            }

            InlineSyncResult inline = reallyNewContact
                    ? syncInlineForNewContact(contactId, row)
                    : syncInlineForExistingContact(contactId, row);

            return new UpsertResult(
                    true,
                    contactId,
                    inline.success(),
                    inline.errorMessage(),
                    reallyNewContact
            );

        } catch (Exception ex) {
            log.error("Error en create de Clientify. phone={}", normalizedPhone, ex);
            return new UpsertResult(false, null, false, ex.getMessage(), false);
        }
    }

    public InlineSyncResult retryInlineSync(Long contactId, VerinaTicketRow row, boolean createdByProject) {
        if (contactId == null || row == null) {
            return new InlineSyncResult(false, "contactId o row nulo");
        }

        return createdByProject
                ? syncInlineForNewContact(contactId, row)
                : syncInlineForExistingContact(contactId, row);
    }

    public Long resolveExistingContactIdByPhone(String phoneE164) {
        String normalizedPhone = phoneNormalizer.toE164OrNull(phoneE164);
        if (normalizedPhone == null || normalizedPhone.isBlank()) return null;
        return findExistingContactIdByPhone(normalizedPhone);
    }

    public boolean addSurveyTagToContact(Long contactId) {
        if (contactId == null) {
            return false;
        }

        try {
            var tagResponse = client.addTagToContact(
                    contactId,
                    new ClientifyClient.TagRequest(encuestaTag())
            );
            log.info("Clientify tag response -> contactId={} response={}", contactId, tagResponse);
            return true;
        } catch (Exception ex) {
            log.warn("Clientify no pudo agregar tag. contactId={} tag={}", contactId, encuestaTag(), ex);
            return false;
        }
    }

    private InlineSyncResult syncInlineForNewContact(Long contactId, VerinaTicketRow row) {
        if (contactId == null || row == null) {
            return new InlineSyncResult(false, "contactId o row nulo");
        }

        List<String> errors = new ArrayList<>();

        captureInlineError(errors, updateOwnerInline(contactId, row.propietario()));
        captureInlineError(errors, updateStatusInline(contactId));
        captureInlineError(errors, updateOrigenInline(contactId, row.origen()));
        captureCustomFieldErrors(errors, contactId, row);

        if (errors.isEmpty()) {
            return new InlineSyncResult(true, null);
        }

        return new InlineSyncResult(false, String.join(" | ", errors));
    }

    private InlineSyncResult syncInlineForExistingContact(Long contactId, VerinaTicketRow row) {
        if (contactId == null || row == null) {
            return new InlineSyncResult(false, "contactId o row nulo");
        }

        List<String> errors = new ArrayList<>();

        captureInlineError(errors, updateOwnerInline(contactId, row.propietario()));
        captureInlineError(errors, updateStatusInline(contactId));
        captureCustomFieldErrors(errors, contactId, row);

        if (errors.isEmpty()) {
            return new InlineSyncResult(true, null);
        }

        return new InlineSyncResult(false, String.join(" | ", errors));
    }

    private void captureCustomFieldErrors(List<String> errors, Long contactId, VerinaTicketRow row) {
        var f = cfg.getCustomFields();

        captureInlineError(errors, updateInline(contactId, f.getFechaUltimaCompraId(), formatFechaUltimaCompra(row)));
        captureInlineError(errors, updateInline(contactId, f.getBateriaAdquiridaId(), row.meBateriaAdquirida()));
        captureInlineError(errors, updateInline(contactId, f.getSucursalId(), row.me14Sucursal()));
        captureInlineError(errors, updateInline(contactId, f.getAnioAutoId(), row.meAnioAuto() == null ? null : String.valueOf(row.meAnioAuto())));
        captureInlineError(errors, updateInline(contactId, f.getModeloAutoId(), row.meModeloAuto()));
        captureInlineError(errors, updateInline(contactId, f.getMarcaAutoId(), row.meMarcaAuto()));
        captureInlineError(errors, updateInline(contactId, f.getMarcaBateriaId(), row.meMarcaBateria()));
        captureInlineError(errors, updateInline(contactId, f.getGamaId(), row.meGama()));
        captureInlineError(errors, updateInline(contactId, f.getFechaFinGarantiaId(), normalizeFechaGarantia(row.meFechaFinGarantia())));
    }

    private void captureInlineError(List<String> errors, InlineActionResult result) {
        if (result != null && !result.success() && result.message() != null && !result.message().isBlank()) {
            errors.add(result.message());
        }
    }

    private InlineActionResult updateOwnerInline(Long contactId, String propietario) {
        if (contactId == null) return new InlineActionResult(false, "owner: contactId nulo");

        Long ownerId = resolveOwnerId(propietario);
        if (ownerId == null || ownerId <= 0) {
            String msg = "owner: no se pudo resolver propietario='" + propietario + "'";
            log.warn(msg);
            return new InlineActionResult(false, msg);
        }

        try {
            String response = executeWithRetry(() ->
                    client.updateInlineField(contactId, "owner", String.valueOf(ownerId))
            );
            log.info(
                    "INLINE OWNER OK contactId={} propietario={} ownerId={} response={}",
                    contactId,
                    propietario,
                    ownerId,
                    response
            );
            return new InlineActionResult(true, null);
        } catch (Exception ex) {
            String msg = "owner: error propietario='" + propietario + "' ownerId=" + ownerId + " -> " + ex.getMessage();
            log.error(
                    "INLINE OWNER ERROR contactId={} propietario={} ownerId={}",
                    contactId,
                    propietario,
                    ownerId,
                    ex
            );
            return new InlineActionResult(false, msg);
        }
    }

    private Long resolveOwnerId(String propietario) {
        if (propietario == null) return null;

        String clean = propietario.trim();
        if (clean.isBlank()) return null;

        if (clean.equalsIgnoreCase("MARTIN S.")) {
            long id = cfg.getOwnerMartinId();
            return id > 0 ? id : null;
        }

        if (clean.equalsIgnoreCase("Daniela Cota")) {
            long id = cfg.getOwnerDanielaId();
            return id > 0 ? id : null;
        }

        return null;
    }

    private InlineActionResult updateStatusInline(Long contactId) {
        String statusValue = cfg.getStatusClientValue();
        if (contactId == null) return new InlineActionResult(false, "status: contactId nulo");
        if (statusValue == null || statusValue.isBlank()) return new InlineActionResult(false, "status: valor vacío");

        try {
            String response = executeWithRetry(() ->
                    client.updateInlineField(contactId, "status", statusValue.trim())
            );
            log.info("INLINE STATUS OK contactId={} value={} response={}", contactId, statusValue, response);
            return new InlineActionResult(true, null);
        } catch (Exception ex) {
            String msg = "status: error value='" + statusValue + "' -> " + ex.getMessage();
            log.error("INLINE STATUS ERROR contactId={} value={}", contactId, statusValue, ex);
            return new InlineActionResult(false, msg);
        }
    }

    private InlineActionResult updateOrigenInline(Long contactId, String origen) {
        if (contactId == null) return new InlineActionResult(false, "origen: contactId nulo");

        Long sourceId = resolveContactSourceId(origen);
        if (sourceId == null || sourceId <= 0) {
            String msg = "origen: no se pudo resolver origen='" + origen + "'";
            log.warn(msg);
            return new InlineActionResult(false, msg);
        }

        try {
            String response = executeWithRetry(() ->
                    client.updateInlineField(contactId, "contact_source", String.valueOf(sourceId))
            );
            log.info(
                    "INLINE ORIGEN OK contactId={} origen={} sourceId={} response={}",
                    contactId,
                    origen,
                    sourceId,
                    response
            );
            return new InlineActionResult(true, null);
        } catch (Exception ex) {
            String msg = "origen: error origen='" + origen + "' sourceId=" + sourceId + " -> " + ex.getMessage();
            log.error(
                    "INLINE ORIGEN ERROR contactId={} origen={} sourceId={}",
                    contactId,
                    origen,
                    sourceId,
                    ex
            );
            return new InlineActionResult(false, msg);
        }
    }

    private Long resolveContactSourceId(String origen) {
        if (origen == null) return null;

        String clean = origen.trim();
        if (clean.isBlank()) return null;

        if (clean.equalsIgnoreCase("piso")) {
            long id = cfg.getContactSourcePisoId();
            return id > 0 ? id : null;
        }

        if (clean.equalsIgnoreCase("domicilio")) {
            long id = cfg.getContactSourceDomicilioId();
            return id > 0 ? id : null;
        }

        return null;
    }

    private InlineActionResult updateInline(Long contactId, long fieldId, String value) {
        if (contactId == null) return new InlineActionResult(false, "field " + fieldId + ": contactId nulo");
        if (fieldId <= 0) return new InlineActionResult(false, "fieldId inválido");
        if (value == null) return new InlineActionResult(true, null);

        String clean = value.trim();
        if (clean.isBlank() || clean.equals("-")) return new InlineActionResult(true, null);

        try {
            String response = executeWithRetry(() -> client.updateCustomFieldInline(contactId, fieldId, clean));
            log.info(
                    "INLINE OK contactId={} fieldId={} value={} response={}",
                    contactId,
                    fieldId,
                    clean,
                    response
            );
            return new InlineActionResult(true, null);
        } catch (Exception ex) {
            String msg = "field " + fieldId + ": error value='" + clean + "' -> " + ex.getMessage();
            log.error(
                    "INLINE ERROR contactId={} fieldId={} value={}",
                    contactId,
                    fieldId,
                    clean,
                    ex
            );
            return new InlineActionResult(false, msg);
        }
    }

    private boolean looksLikePreexistingContact(ClientifyClient.ClientifyContact contact) {
        if (contact == null) return false;

        if (contact.contact_source() != null && !contact.contact_source().isBlank()) {
            return true;
        }

        if (contact.phones() != null && contact.phones().size() > 1) {
            return true;
        }

        if (contact.tags() != null) {
            String encuesta = encuestaTag().toLowerCase(Locale.ROOT);

            long otherTags = contact.tags().stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                    .filter(tag -> !tag.equals(encuesta))
                    .count();

            if (otherTags > 0) {
                return true;
            }
        }

        if (contact.status() != null && !contact.status().isBlank()) {
            String status = contact.status().trim().toLowerCase(Locale.ROOT);
            if (!status.equals("cold-lead")) {
                return true;
            }
        }

        if (contact.custom_fields() != null && !contact.custom_fields().isEmpty()) {
            return true;
        }

        return false;
    }

    private String formatFechaUltimaCompra(VerinaTicketRow row) {
        if (row == null || row.fechaUltimaCompra() == null) return null;
        return row.fechaUltimaCompra().toLocalDate().format(DATE_DD_MM_YYYY);
    }

    private String normalizeFechaGarantia(String raw) {
        if (raw == null) return null;

        String clean = raw.trim();
        if (clean.isBlank() || clean.equals("-")) return null;

        if (clean.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] p = clean.split("-");
            return p[2] + "/" + p[1] + "/" + p[0];
        }

        return clean;
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

    private String encuestaTag() {
        String tag = cfg.getTags().getEncuestaSatisfaccion();
        return (tag == null || tag.isBlank()) ? "encuesta_postventa" : tag.trim();
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

    public record UpsertResult(
            boolean ok,
            Long contactId,
            boolean inlineSyncOk,
            String inlineError,
            boolean createdNewContact
    ) {}

    public record InlineSyncResult(
            boolean success,
            String errorMessage
    ) {}

    private record InlineActionResult(
            boolean success,
            String message
    ) {}

    private record SearchVariants(
            String plus52,
            String plus521,
            String local10,
            String comparable
    ) {}
}