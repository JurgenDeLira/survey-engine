package com.batteryplus.survey.connector.verina;

import com.batteryplus.survey.core.model.VerinaTicketRow;
import org.springframework.jdbc.core.RowMapper;

public class VerinaRowMappers {

    private VerinaRowMappers() {}

    public static final RowMapper<VerinaTicketRow> VERINA_TICKET_ROW = (rs, rowNum) -> new VerinaTicketRow(
            rs.getInt("IDSucursal"),
            rs.getString("Sucursal"),
            rs.getLong("Ticket"),
            rs.getTimestamp("ME_Fecha_ultima_compra").toLocalDateTime(),

            rs.getString("Propietario"),
            rs.getString("ME_Gama"),
            rs.getString("ME_Marca_bateria"),
            rs.getString("ME_Bateria_adquirida"),

            rs.getString("Nombre"),
            rs.getString("Apellido"),
            rs.getString("Telefono"),
            rs.getString("Correo_electronico"),

            rs.getString("ME_Marca_auto"),
            rs.getString("ME_Modelo_auto"),
            rs.getObject("ME_Anio_auto", Integer.class),
            rs.getString("ME_Fecha_fin_garantia"),

            rs.getString("Pais"),
            rs.getString("Estado_Provincia"),
            rs.getString("Ciudad"),
            rs.getString("Origen"),
            rs.getString("Estado")
    );
}