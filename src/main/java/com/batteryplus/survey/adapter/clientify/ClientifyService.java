package com.batteryplus.survey.adapter.clientify;

import com.batteryplus.survey.config.ClientifyConfig;
import com.batteryplus.survey.core.model.VerinaTicketRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientifyService {

    private static final Logger log = LoggerFactory.getLogger(ClientifyService.class);
    private static final Integer PHONE_TYPE_MOBILE = 1;

    private final ClientifyClient client;
    private final ClientifyMapper mapper;
    private final ClientifyConfig cfg;

    public ClientifyService(ClientifyClient client, ClientifyMapper mapper, ClientifyConfig cfg) {
        this.client = client;
        this.mapper = mapper;
        this.cfg = cfg;
    }

    public boolean upsertContactFromSale(String phoneE164, String ticketValue, VerinaTicketRow row) {
        if (phoneE164 == null || phoneE164.isBlank()) return false;
        if (ticketValue == null || ticketValue.isBlank()) return false;
        if (row == null) return false;

        Long contactId = findContactIdByPhone(phoneE164);

        if (contactId == null) {
            contactId = createContact(phoneE164, row);
            if (contactId == null) {
                log.warn("No se pudo crear contacto en Clientify. phone={}", phoneE164);
                return false;
            }
            log.info("Contacto creado en Clientify. contactId={} phone={}", contactId, phoneE164);
        } else {
            log.info("Contacto encontrado en Clientify. contactId={} phone={}", contactId, phoneE164);
        }

        long fieldId = cfg.getCustomFields().getUltimaCompraTicketId();

        var putPayload = new ClientifyClient.PutContactRequest(
                safe(row.nombre()),
                safe(row.apellido()),
                safeNullable(row.correoElectronico()),
                List.of(new ClientifyClient.CustomFieldValue(fieldId, ticketValue))
        );

        log.info("Clientify PUT -> contactId={} fieldId={} ticketValue={}", contactId, fieldId, ticketValue);

        var putResponse = client.putContact(contactId, putPayload);

        log.info("Clientify PUT response -> contactId={} response={}", contactId, putResponse);

        var tagResponse = client.addTagToContact(
                contactId,
                new ClientifyClient.TagRequest(encuestaTag())
        );

        log.info("Clientify tag response -> contactId={} response={}", contactId, tagResponse);

        return true;
    }

    private Long findContactIdByPhone(String phoneE164) {
        var search = client.searchContacts(phoneE164, 20);
        return mapper.pickContactIdByExactPhone(search, phoneE164);
    }

    private Long createContact(String phoneE164, VerinaTicketRow row) {
        String firstName = safe(row.nombre());
        String lastName = safe(row.apellido());
        String email = safeNullable(row.correoElectronico());

        var payload = new ClientifyClient.CreateContactRequest(
                firstName,
                lastName,
                email,
                List.of(new ClientifyClient.CreatePhone(PHONE_TYPE_MOBILE, phoneE164)),
                List.of(encuestaTag())
        );

        log.info(
                "Clientify create -> firstName={} lastName={} email={} phone={} tag={}",
                firstName, lastName, email, phoneE164, encuestaTag()
        );

        var created = client.createContact(payload);

        log.info("Clientify create response -> response={}", created);

        return created != null ? created.id() : null;
    }

    private String encuestaTag() {
        String tag = cfg.getTags().getEncuestaSatisfaccion();
        return (tag == null || tag.isBlank()) ? "encuesta_satisfaccion" : tag.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}