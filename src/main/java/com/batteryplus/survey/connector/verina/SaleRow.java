package com.batteryplus.survey.connector.verina;

import java.time.LocalDateTime;

public record SaleRow(
        int idSucursal,
        String sucursal,
        long ticket,
        LocalDateTime fecha,
        String nombreAutomovilista,
        String telefono,
        String email,
        String familia,
        String marca,
        String producto,
        Integer cantidad
) {
}
