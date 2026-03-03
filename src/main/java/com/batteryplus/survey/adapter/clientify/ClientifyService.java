package com.batteryplus.survey.adapter.clientify;

//upsert + tag + campos
import com.batteryplus.survey.config.ClientifyConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ClientifyService {

    private static final String TAG_ENCUESTA = "encuesta_satisfaccion";

    private final ClientifyClient client;
    private final ClientifyMapper mapper;
    private final ClientifyConfig cfg;

    public ClientifyService(ClientifyClient client, ClientifyMapper mapper, ClientifyConfig cfg) {
        this.client = client;
        this.mapper = mapper;
        this.cfg = cfg;
    }

    /**
     * - Busca contacto por teléfono
     * - Actualiza el custom field "ME_Ultima compra ticket"
     * - Agrega tag "encuesta_satisfaccion" sin borrar tags existentes
     */
    public boolean upsertUltimaCompraTicketAndTagByPhone(String phoneE164, String ticketValue) {
        if (phoneE164 == null || phoneE164.isBlank()) return false;
        if (ticketValue == null || ticketValue.isBlank()) return false;

        // 1) Buscar contacto
        var search = client.searchContacts(phoneE164, 20);
        Long contactId = mapper.pickContactIdByExactPhone(search, phoneE164);
        if (contactId == null) return false;

        // 2) Traer contacto actual (para no pisar tags)
        var existing = client.getContact(contactId);

        // 3) Merge tags (mantener orden y sin duplicados)
        List<String> mergedTags = mergeTags(existing != null ? existing.tags() : null, TAG_ENCUESTA);

        // 4) Construir payload
        long fieldId = cfg.getCustomFields().getUltimaCompraTicketId();
        var payload = new ClientifyClient.PatchContactRequest(
                List.of(new ClientifyClient.CustomFieldValue(fieldId, ticketValue)),
                mergedTags
        );

        // 5) PATCH
        client.patchContact(contactId, payload);
        return true;
    }

    private List<String> mergeTags(List<String> existing, String newTag) {
        var set = new LinkedHashSet<String>();
        if (existing != null) {
            for (String t : existing) {
                if (t != null && !t.isBlank()) set.add(t.trim());
            }
        }
        if (newTag != null && !newTag.isBlank()) set.add(newTag.trim());
        return new ArrayList<>(set);
    }
}