package com.batteryplus.survey.core.model;

import java.time.LocalDateTime;

public record VerinaSaleRow(
        String ticket,          // t.Numero
        Integer idSucursal,      // t.IDSucursal
        String sucursal,         // t.Sucursal
        LocalDateTime fecha,     // t.Fecha
        String producto,         // t.Producto
        Integer cantidad,        // t.Cantidad
        String nombre,           // Nombre_Automovilista
        String telefono,         // g.Telefono
        String email             // g.Email
) { }