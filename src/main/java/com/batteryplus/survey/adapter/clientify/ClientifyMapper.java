package com.batteryplus.survey.adapter.clientify;

//domain -> payload
import org.springframework.stereotype.Component;

@Component
public class ClientifyMapper {

    public Long pickContactIdByExactPhone(ClientifyClient.ClientifyContactSearch search, String phoneE164) {
        if (search == null || search.results() == null) return null;

        for (var r : search.results()) {
            if (r == null || r.phones() == null) continue;

            for (var p : r.phones()) {
                if (p != null && phoneE164.equals(p.phone())) {
                    return r.id();
                }
            }
        }
        return null;
    }
}