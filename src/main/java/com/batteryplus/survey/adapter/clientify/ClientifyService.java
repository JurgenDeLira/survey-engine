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

    private final ClientifyClient client;
    private final ClientifyConfig cfg;

    public ClientifyService(ClientifyClient client, ClientifyConfig cfg) {
        this.client = client;
        this.cfg = cfg;
    }

    /**
     * Hace upsert práctico en Clientify usando POST /contacts/.
     * Si el teléfono/email ya existe, Clientify debería actualizar el mismo contacto.
     * Después intenta agregar la tag por endpoint separado.
     *
     * Nota:
     * Aunque mandamos custom_fields, hoy Clientify los está ignorando en nuestras pruebas.
     * Por eso dejamos warning explícito si no vienen reflejados en la respuesta.
     */
    public boolean upsertContactFromSale(String phoneE164, String ticketValue, VerinaTicketRow row) {
        if (phoneE164 == null || phoneE164.isBlank()) return false;
        if (ticketValue == null || ticketValue.isBlank()) return false;
        if (row == null) return false;

        long fieldId = cfg.getCustomFields().getUltimaCompraTicketId();

        var payload = new ClientifyClient.UpsertContactRequest(
                safe(row.nombre()),
                safe(row.apellido()),
                safeNullable(row.correoElectronico()),
                phoneE164,
                List.of(new ClientifyClient.CustomFieldValue(fieldId, ticketValue)),
                List.of(encuestaTag())
        );

        log.info(
                "Clientify POST upsert -> phone={} fieldId={} ticketValue={} tag={}",
                phoneE164, fieldId, ticketValue, encuestaTag()
        );

        var response = client.upsertContact(payload);

        log.info("Clientify POST upsert response -> response={}", response);

        if (response == null || response.id() == null) {
            log.warn("Clientify no regresó contacto válido. phone={} ticketValue={}", phoneE164, ticketValue);
            return false;
        }

        Long contactId = response.id();

        try {
            var tagResponse = client.addTagToContact(
                    contactId,
                    new ClientifyClient.TagRequest(encuestaTag())
            );
            log.info("Clientify tag response -> contactId={} response={}", contactId, tagResponse);
        } catch (Exception ex) {
            log.warn("Clientify no pudo agregar tag. contactId={} tag={}", contactId, encuestaTag(), ex);
        }

        if (response.custom_fields() == null || response.custom_fields().isEmpty()) {
            log.warn(
                    "Clientify aceptó la petición pero no reflejó custom_fields. contactId={} fieldId={} ticketValue={}",
                    contactId, fieldId, ticketValue
            );
        }

        return true;
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