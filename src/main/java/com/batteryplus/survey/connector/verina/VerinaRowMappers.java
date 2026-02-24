package com.batteryplus.survey.connector.verina;

//RowMappers
import org.springframework.jdbc.core.RowMapper;
import java.sql.Timestamp;

public class VerinaRowMappers {
    private VerinaRowMappers() {}

    public static final RowMapper<SaleRow> SALE_ROW = (rs, rowNum) -> {
        Timestamp ts = rs.getTimestamp("Fecha");

        return new SaleRow(
                rs.getInt("IDSucursal"),
                rs.getString("Sucursal"),
                rs.getLong("Ticket"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getString("Nombre_Automovilista"),
                rs.getString("Telefono"),
                rs.getString("Email"),
                rs.getString("Familia"),
                rs.getString("Marca"),
                rs.getString("Producto"),
                (Integer) rs.getObject("Cantidad") // null-safe
        );
    };
}