package com.batteryplus.survey.adapter.clientify;

//domain -> payload
import org.springframework.stereotype.Component;

@Component
public class ClientifyMapper {

    public Long pickContactIdByExactPhone(ClientifyClient.ClientifyContactSearch search, String phoneE164) {
        if (search == null || search.results() == null || phoneE164 == null) return null;

        String target = normalize(phoneE164);
        Long found = null;

        for (var r : search.results()) {
            if (r == null || r.phones() == null) continue;

            for (var p : r.phones()) {
                if (p == null || p.phone() == null) continue;

                if (target.equals(normalize(p.phone()))) {
                    if (found != null && !found.equals(r.id())) {
                        // duplicado
                        return null;
                    }
                    found = r.id();
                }
            }
        }
        return found;
    }

    private String normalize(String s) {
        if (s == null) return null;
        // deja solo + y dígitos
        String cleaned = s.trim().replaceAll("[^0-9+]", "");
        // si trae + en medio, quítalo y deja solo al inicio
        if (cleaned.indexOf('+') > 0) cleaned = cleaned.replace("+", "");
        if (!cleaned.isEmpty() && cleaned.charAt(0) != '+' && s.trim().startsWith("+")) {
            cleaned = "+" + cleaned;
        }
        return cleaned;
    }

    public String toE164OrNull(String raw) {
        if (raw == null) return null;

        String digits = raw.replaceAll("\\D", ""); // solo números

        // 10 dígitos MX (local)
        if (digits.length() == 10) {
            return "+521" + digits;
        }

        // 11 dígitos si viene con "1" ya pegado (ej: 1668...)
        if (digits.length() == 11 && digits.startsWith("1")) {
            return "+52" + digits; // quedaría +521...
        }

        // 12 dígitos si viene como 52 + 10
        if (digits.length() == 12 && digits.startsWith("52")) {
            // muchos sistemas lo guardan como +521...
            return "+521" + digits.substring(2);
        }

        // ya viene con country?
        if (digits.length() >= 11 && raw.trim().startsWith("+")) {
            return "+" + digits;
        }

        return null;
    }
}