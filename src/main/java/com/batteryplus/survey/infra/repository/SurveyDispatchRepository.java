package com.batteryplus.survey.infra.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@ConditionalOnProperty(prefix = "app.datasource.staging", name = "enabled", havingValue = "true")
@Repository
public class SurveyDispatchRepository {

    private final JdbcTemplate stagingJdbc;

    public SurveyDispatchRepository(@Qualifier("stagingJdbcTemplate") JdbcTemplate stagingJdbc) {
        this.stagingJdbc = stagingJdbc;
    }

    public List<PendingSurveyRow> findPending(int limit) {
        return stagingJdbc.query("""
            SELECT TOP (?)
                purchase_id,
                telefono,
                nombre,
                propietario,
                sucursal
            FROM dbo.purchase_events
            WHERE survey_status = 'pending'
            ORDER BY fecha ASC, created_at ASC
            """,
                (rs, rowNum) -> new PendingSurveyRow(
                        rs.getString("purchase_id"),
                        rs.getString("telefono"),
                        rs.getString("nombre"),
                        rs.getString("propietario"),
                        rs.getString("sucursal")
                ),
                limit
        );
    }

    public boolean updateStatus(String purchaseId, String newStatus) {
        int updated = stagingJdbc.update("""
            UPDATE dbo.purchase_events
            SET survey_status = ?
            WHERE purchase_id = ?
            """, newStatus, purchaseId);

        return updated > 0;
    }

    public List<ReadySurveyRow> findReadyToDispatch(int limit) {
        return stagingJdbc.query("""
            SELECT TOP (?)
                purchase_id,
                telefono,
                nombre,
                propietario,
                sucursal,
                clientify_contact_id
            FROM dbo.purchase_events
            WHERE survey_status = 'ready_for_campaign'
            ORDER BY fecha ASC, created_at ASC
            """,
                (rs, rowNum) -> new ReadySurveyRow(
                        rs.getString("purchase_id"),
                        rs.getString("telefono"),
                        rs.getString("nombre"),
                        rs.getString("propietario"),
                        rs.getString("sucursal"),
                        rs.getObject("clientify_contact_id", Long.class)
                ),
                limit
        );
    }

    public boolean markDispatched(String purchaseId) {
        int updated = stagingJdbc.update("""
            UPDATE dbo.purchase_events
            SET survey_status = 'dispatched'
            WHERE purchase_id = ?
              AND survey_status = 'ready_for_campaign'
            """, purchaseId);

        return updated > 0;
    }

    public record PendingSurveyRow(
            String purchaseId,
            String telefono,
            String nombre,
            String propietario,
            String sucursal
    ) {}

    public record ReadySurveyRow(
            String purchaseId,
            String telefono,
            String nombre,
            String propietario,
            String sucursal,
            Long clientifyContactId
    ) {}
}