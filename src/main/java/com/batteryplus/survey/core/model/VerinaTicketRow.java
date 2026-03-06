package com.batteryplus.survey.core.model;

import java.time.LocalDateTime;

/**
 * Fila mínima para detectar una venta y poder generar la encuesta.
 */
public record VerinaTicketRow(
        int idSucursal,
        String sucursal,
        long ticket,
        LocalDateTime fecha,
        String nombreAutomovilista,
        String telefono,
        String email
) {
    public String naturalKey() {
        return idSucursal + "-" + ticket;
    }
}