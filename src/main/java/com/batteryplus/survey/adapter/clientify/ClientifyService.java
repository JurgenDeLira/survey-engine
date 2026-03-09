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

    private static final String TAG_ENCUESTA = "encuesta_satisfaccion";
    private static final Integer PHONE_TYPE_MOBILE = 1;

    private final ClientifyClient client;
    private final ClientifyMapper mapper;
    private final ClientifyConfig cfg;

    public ClientifyService(ClientifyClient client, ClientifyMapper mapper, ClientifyConfig cfg) {
        this.client = client;
        this.mapper = mapper;
        this.cfg = cfg;
    }

    /**
     * Busca contacto por teléfono.
     * Si no existe, lo crea.
     * Luego actualiza el custom field "ME_Ultima compra ticket"
     * y deja la tag "encuesta_satisfaccion".
     */
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
        }

        long fieldId = cfg.getCustomFields().getUltimaCompraTicketId();

        var payload = new ClientifyClient.PatchContactRequest(
                List.of(new ClientifyClient.CustomFieldValue(fieldId, ticketValue)),
                List.of(TAG_ENCUESTA)
        );

        client.patchContact(contactId, payload);
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
                List.of(TAG_ENCUESTA)
        );

        var created = client.createContact(payload);
        return created != null ? created.id() : null;
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