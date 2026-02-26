package com.batteryplus.survey.core.normalize;

//normaliza teléfonos (dedupe)
import org.springframework.stereotype.Component;

@Component
public class PhoneNormalizer {

    public String toE164OrNull(String raw) {

        if (raw == null) return null;

        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;

        // Si ya empieza con 52 (ej 521668...)
        if (digits.startsWith("52")) {
            return "+" + digits;
        }

        // Si son 10 dígitos (ej 6681234567)
        if (digits.length() == 10) {
            return "+52" + digits;
        }

        // Fallback
        return "+" + digits;
    }
}