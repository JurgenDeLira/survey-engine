package com.batteryplus.survey.adapter.clientify;

//upsert + tag + campos
import com.batteryplus.survey.config.ClientifyConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ClientifyService {

    private final ClientifyClient client;
    private final ClientifyMapper mapper;
    private final ClientifyConfig cfg;

    public ClientifyService(ClientifyClient client, ClientifyMapper mapper, ClientifyConfig cfg) {
        this.client = client;
        this.mapper = mapper;
        this.cfg = cfg;
    }

    /**
     * 1) Busca el contacto por teléfono.
     * 2) Si existe:
     *    - actualiza custom field "ME_Ultima compra ticket"
     *    - asegura tag "encuesta_satisfaccion" sin borrar tags previos
     */
    public boolean upsertUltimaCompraTicketAndTagByPhone(String phoneE164, String ticketValue) {
        var search = client.searchContacts(phoneE164, 20);
        Long contactId = mapper.pickContactIdByExactPhone(search, phoneE164);
        if (contactId == null) return false;

        var existing = client.getContact(contactId);
        List<String> mergedTags = mergeTags(existing.tags(), cfg.getTags().getEncuestaSatisfaccion());

        long fieldId = cfg.getCustomFields().getUltimaCompraTicketId();

        var payload = new ClientifyClient.PatchContactRequest(
                List.of(new ClientifyClient.CustomFieldValue(fieldId, ticketValue)),
                mergedTags
        );

        client.patchContact(contactId, payload);
        return true;
    }

    private static List<String> mergeTags(List<String> current, String requiredTag) {
        var set = new LinkedHashSet<String>();
        if (current != null) set.addAll(current);
        if (requiredTag != null && !requiredTag.isBlank()) set.add(requiredTag);
        return new ArrayList<>(set);
    }
}