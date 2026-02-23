package com.batteryplus.survey.connector.verina;

//lee de Verina (JdbcTemplate)

import com.batteryplus.survey.core.model.Customer;
import com.batteryplus.survey.core.model.PurchaseEvent;
import com.batteryplus.survey.core.model.PurchaseItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.List;

@Component
public class VerinaPurchaseReader {

    private final JdbcTemplate verinaJdbc;

    public VerinaPurchaseReader(JdbcTemplate verinaJdbcTemplate) {
        this.verinaJdbc = verinaJdbcTemplate;
    }

    public List<PurchaseEvent> fetchPurchaseAfterTicket (long lastTicketId, int limit) {
        // TODO: Reemplazar mi SQL real + columnas reales
        // Idea: Traer ventas nuevas por ticket id.
        String sql = """
                SELECT TOP (?)
                    v.Ticket AS ticket_id,
                    v.IDSucursal AS branch_id,
                    s.Nombre AS branch_name,
                    v.Fecha AS purchase_datetime,
                    c.Nombre_Automovilista AS customer_name,
                    c.Telefono AS customer_phone,
                    c.Email AS customer_email
                FROM Ventas v
                JOIN Sucursales s ON s.IDSucursal = v.IDSucursal
                LEFT JOIN Clientes c ON c.IDCliente = v.IDCliente
                WHERE v.Ticket > ?
                ORDER BY v.Ticket ASC
                """;

        return verinaJdbc.query(sql, (rs, rowNum) -> {
            long ticketId = rs.getLong("ticket_id");
            int branchId = rs.getInt("branch_id");
            String branchName = rs.getString("branch_name");

            OffsetDateTime dt = rs.getTimestamp("purchase_datetime").toInstant()
                    .atOffset(OffsetTime.now().getOffset());

            Customer customer = new Customer(
                    rs.getString("customer_name"),
                    rs.getString("customer_phone"),
                    rs.getString("customer_email")
            );

            // MVP: items vacío. Luego lo lleno con otro query por ticket.
            List<PurchaseItem> items = List.of();

            String purchaseId = "VERINA-" + branchId + "-" + ticketId;

            return new PurchaseEvent(
                    purchaseId,
                    "VERINA",
                    ticketId,
                    branchId,
                    branchName,
                    dt,
                    customer,
                    items
            );
        }, limit, lastTicketId);
    }
}
