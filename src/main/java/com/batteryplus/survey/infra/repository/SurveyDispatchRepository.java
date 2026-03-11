package com.batteryplus.survey.infra.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
                pe.fecha,
                pe.id_sucursal,
                pe.sucursal,
                pe.ticket,
                pe.telefono,
                pe.nombre,
                pe.email,
                pe.propietario,
                pe.marca,
                pe.producto,
                pe.me_gama,
                pe.me_marca_auto,
                pe.me_modelo_auto,
                pe.me_anio_auto,
                pe.me_fecha_fin_garantia,
                pe.pais,
                pe.estado_provincia,
                pe.ciudad,
                pe.origen,
                pe.estado
            FROM dbo.purchase_events pe
            LEFT JOIN dbo.survey_checkpoint sc
                ON sc.ticket_id = pe.purchase_id
            WHERE sc.ticket_id IS NULL
              AND pe.telefono IS NOT NULL
              AND LTRIM(RTRIM(pe.telefono)) <> ''
            ORDER BY pe.fecha ASC, pe.purchase_id ASC
            """,
                (rs, rowNum) -> new PendingSurveyRow(
                        rs.getString("purchase_id"),
                        rs.getTimestamp("fecha").toLocalDateTime(),
                        rs.getInt("id_sucursal"),
                        rs.getString("sucursal"),
                        rs.getLong("ticket"),
                        rs.getString("telefono"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getString("propietario"),
                        rs.getString("marca"),
                        rs.getString("producto"),
                        rs.getString("me_gama"),
                        rs.getString("me_marca_auto"),
                        rs.getString("me_modelo_auto"),
                        rs.getObject("me_anio_auto", Integer.class),
                        rs.getString("me_fecha_fin_garantia"),
                        rs.getString("pais"),
                        rs.getString("estado_provincia"),
                        rs.getString("ciudad"),
                        rs.getString("origen"),
                        rs.getString("estado")
                ),
                limit
        );
    }

    public boolean markPrepared(String purchaseId) {
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
            LocalDateTime fecha,
            int idSucursal,
            String sucursal,
            long ticket,
            String telefono,
            String nombre,
            String email,
            String propietario,
            String marca,
            String producto,
            String meGama,
            String meMarcaAuto,
            String meModeloAuto,
            Integer meAnioAuto,
            String meFechaFinGarantia,
            String pais,
            String estadoProvincia,
            String ciudad,
            String origen,
            String estado
    ) {}
}