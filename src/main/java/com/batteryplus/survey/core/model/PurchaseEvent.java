package com.batteryplus.survey.core.model;

import java.time.LocalDateTime;

//modelo canónico
public record PurchaseEvent(
        LocalDateTime fechaGarantia,
        Integer noGarantia
) {}
