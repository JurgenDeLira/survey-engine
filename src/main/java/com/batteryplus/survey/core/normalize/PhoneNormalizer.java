package com.batteryplus.survey.core.normalize;

import org.springframework.stereotype.Component;

@Component
public class PhoneNormalizer {

    /**
     * Formato principal para guardar/enviar a Clientify:
     * MX -> +52 + 10 dígitos
     */
    public String toE164OrNull(String raw) {
        if (raw == null) return null;

        String digits = raw.replaceAll("\\D", "");

        // MX local: 10 dígitos
        if (digits.length() == 10) {
            return "+52" + digits;
        }

        // MX con 52 + 10
        if (digits.length() == 12 && digits.startsWith("52")) {
            return "+" + digits;
        }

        // MX con 521 + 10  -> normalizar a +52 + 10
        if (digits.length() == 13 && digits.startsWith("521")) {
            return "+52" + digits.substring(3);
        }

        // internacional ya con +
        if (raw.trim().startsWith("+") && digits.length() >= 11) {
            return "+" + digits;
        }

        return null;
    }

    /**
     * Formato comparable para detectar equivalencias entre +52 y +521.
     * Devuelve solo dígitos.
     */
    public String comparable(String raw) {
        if (raw == null) return null;

        String digits = raw.replaceAll("\\D", "");

        // +521XXXXXXXXXX -> 52XXXXXXXXXX
        if (digits.length() == 13 && digits.startsWith("521")) {
            return "52" + digits.substring(3);
        }

        // +52XXXXXXXXXX
        if (digits.length() == 12 && digits.startsWith("52")) {
            return digits;
        }

        // local MX 10 dígitos -> 52 + 10
        if (digits.length() == 10) {
            return "52" + digits;
        }

        return digits;
    }
}