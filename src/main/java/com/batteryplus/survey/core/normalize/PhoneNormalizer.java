package com.batteryplus.survey.core.normalize;

//normaliza teléfonos (dedupe)
import org.springframework.stereotype.Component;

@Component
public class PhoneNormalizer {

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