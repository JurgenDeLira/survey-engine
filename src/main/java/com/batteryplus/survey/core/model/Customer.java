package com.batteryplus.survey.core.model;

import java.time.LocalDateTime;

public record Customer(
        String nombreAutomovilista,
        String telefono,
        String email,
        String marcaCarro,
        String tipo,
        String modelo,
        LocalDateTime fechaGarantia,
        Integer noGarantia
) { }
