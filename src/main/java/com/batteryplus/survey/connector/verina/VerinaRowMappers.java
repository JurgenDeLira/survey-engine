package com.batteryplus.survey.connector.verina;

//RowMappers
import com.batteryplus.survey.core.model.VerinaTicketRow;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;

public class VerinaRowMappers {

    private VerinaRowMappers() {}

    public static final RowMapper<VerinaTicketRow> VERINA_TICKET_ROW = (rs, rowNum) -> new VerinaTicketRow(
            rs.getInt("IDSucursal"),
            rs.getString("Sucursal"),
            rs.getLong("Ticket"),
            rs.getTimestamp("Fecha").toLocalDateTime(),
            rs.getString("Nombre_Automovilista"),
            rs.getString("Telefono"),
            rs.getString("Email")
    );
}