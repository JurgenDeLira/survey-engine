package com.batteryplus.survey.infra.repository;

import com.batteryplus.survey.core.model.Customer;
import com.batteryplus.survey.core.model.PurchaseEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

//inserta/consulta eventos
@Repository
public class PurchaseEventRepository {

    private final JdbcTemplate stagingJdbc;

    public PurchaseEventRepository(JdbcTemplate stagingJdbcTemplate) {
        this.stagingJdbc = stagingJdbcTemplate;
    }

    public void insertIfNotExists(PurchaseEvent event, String customerKey, String payLoadJson) {
        Customer c = event.customer();

        //Insert idempotente: si ya tengo purchase_id, no lo duplico
        stagingJdbc.update("""
                IF NOT EXISTS (SELECT 1 FROM dbo.purchase_events WHERE purchase_id = ?)
                BEGIN
                    INSERT INTO dbo.purchase_events (
                    purchase_id, source, ticket_id, branch_id, branch_name, purchase_datetime, 
                    customer_key, customer_name, customer_phone, customer_email, payload_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    END
                """,
                event.purchaseId(),
                event.purchaseid(), event.source(), event.ticketId(), event.branchId(),
                event.branchName(), event.purchaseDateTime().toInstant(), customerKey, c != null c.name() : null,
                c != null ? c.phone() : null, c != null ? c.email() : null, payLoadJson
        );
    }
}
