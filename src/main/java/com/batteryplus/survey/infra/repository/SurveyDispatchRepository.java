package com.batteryplus.survey.infra.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
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
                pe.purchase_id,
                pe.telefono,
                pe.nombre,
                pe.sucursal,
                pe.producto,
                pe.fecha
            FROM dbo.purchase_events pe
            LEFT JOIN dbo.survey_checkpoint sc
                ON sc.ticket_id = pe.purchase_id
            WHERE sc.ticket_id IS NULL
              AND pe.telefono IS NOT NULL
              AND LTRIM(RTRIM(pe.telefono)) <> ''
            ORDER BY pe.fecha ASC
            """,
                (rs, rowNum) -> new PendingSurveyRow(
                        rs.getString("purchase_id"),
                        rs.getString("telefono"),
                        rs.getString("nombre"),
                        rs.getString("sucursal"),
                        rs.getString("producto"),
                        rs.getTimestamp("fecha").toLocalDateTime()
                ),
                limit
        );
    }

    public boolean markDispatched(String purchaseId) {
        try {
            stagingJdbc.update("""
                INSERT INTO dbo.survey_checkpoint (ticket_id)
                VALUES (?)
                """, purchaseId);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public record PendingSurveyRow(
            String purchaseId,
            String telefono,
            String nombre,
            String sucursal,
            String producto,
            java.time.LocalDateTime fecha
    ) {}
}