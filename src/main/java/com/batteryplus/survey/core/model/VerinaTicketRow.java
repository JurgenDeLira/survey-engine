package com.batteryplus.survey.core.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Fila normalizada desde Verina para sincronizar a Clientify.
 */
public record VerinaTicketRow(
        int idSucursal,
        String sucursal,
        String me14Sucursal,
        long ticket,
        LocalDateTime fechaUltimaCompra,

        String propietario,
        String meGama,
        String meMarcaBateria,
        String meBateriaAdquirida,

        String nombre,
        String apellido,
        String telefono,
        String correoElectronico,

        String meMarcaAuto,
        String meModeloAuto,
        Integer meAnioAuto,
        String meFechaFinGarantia,

        String pais,
        String estadoProvincia,
        String ciudad,
        String origen,
        String estado
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String naturalKey() {
        return idSucursal + "-" + ticket;
    }

    public LocalDateTime fechaCreacion() {
        return fechaUltimaCompra;
    }

    /**
     * Convierte LocalDateTime a String para Clientify
     */
    public String meFechaUltimaCompra() {
        if (fechaUltimaCompra == null) return null;
        return fechaUltimaCompra.format(FORMATTER);
    }
}