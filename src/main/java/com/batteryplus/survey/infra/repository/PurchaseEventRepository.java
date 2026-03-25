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
public class PurchaseEventRepository {

    private final JdbcTemplate stagingJdbc;

    public PurchaseEventRepository(@Qualifier("stagingJdbcTemplate") JdbcTemplate stagingJdbc) {
        this.stagingJdbc = stagingJdbc;
    }

    public boolean insertIfNotExists(
            String purchaseId,
            String source,
            LocalDateTime fecha,
            int idSucursal,
            String sucursal,
            long ticket,
            String telefono,
            String nombre,
            String email,
            String propietario,
            String familia,
            String marca,
            String producto,
            Integer cantidad,
            String meGama,
            String meMarcaAuto,
            String meModeloAuto,
            Integer meAnioAuto,
            String meFechaFinGarantia,
            String pais,
            String estadoProvincia,
            String ciudad,
            String origen,
            String estado,
            String payloadJson
    ) {
        try {
            stagingJdbc.update("""
                INSERT INTO dbo.purchase_events (
                    purchase_id, source, fecha, id_sucursal, sucursal, ticket,
                    telefono, nombre, email, propietario,
                    familia, marca, producto, cantidad,
                    me_gama, me_marca_auto, me_modelo_auto, me_anio_auto, me_fecha_fin_garantia,
                    pais, estado_provincia, ciudad, origen, estado,
                    survey_status,
                    clientify_contact_id,
                    clientify_inline_synced,
                    clientify_inline_attempts,
                    clientify_inline_last_error,
                    payload_json
                ) VALUES (?, ?, ?, ?, ?, ?,
                         ?, ?, ?, ?,
                         ?, ?, ?, ?,
                         ?, ?, ?, ?, ?,
                         ?, ?, ?, ?, ?,
                         ?,
                         ?,
                         ?,
                         ?,
                         ?,
                         ?)
                """,
                    purchaseId,
                    source,
                    Timestamp.valueOf(fecha),
                    idSucursal,
                    sucursal,
                    ticket,
                    telefono,
                    nombre,
                    email,
                    propietario,
                    familia,
                    marca,
                    producto,
                    cantidad,
                    meGama,
                    meMarcaAuto,
                    meModeloAuto,
                    meAnioAuto,
                    meFechaFinGarantia,
                    pais,
                    estadoProvincia,
                    ciudad,
                    origen,
                    estado,
                    "pending",
                    null,
                    0,
                    0,
                    null,
                    payloadJson
            );
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public void markClientifyInlineSyncSuccess(String purchaseId, Long contactId) {
        stagingJdbc.update("""
            UPDATE dbo.purchase_events
               SET clientify_contact_id = ?,
                   clientify_inline_synced = 1,
                   clientify_inline_last_error = NULL
             WHERE purchase_id = ?
            """,
                contactId,
                purchaseId
        );
    }

    public void markClientifyInlineSyncFailed(String purchaseId, Long contactId, String error) {
        stagingJdbc.update("""
            UPDATE dbo.purchase_events
               SET clientify_contact_id = COALESCE(?, clientify_contact_id),
                   clientify_inline_synced = 0,
                   clientify_inline_attempts = ISNULL(clientify_inline_attempts, 0) + 1,
                   clientify_inline_last_error = ?
             WHERE purchase_id = ?
            """,
                contactId,
                truncate(error, 1000),
                purchaseId
        );
    }

    public List<PendingInlineSyncRow> findPendingInlineSync(int limit) {
        return stagingJdbc.query("""
            SELECT TOP (?)
                   purchase_id,
                   telefono,
                   payload_json,
                   clientify_contact_id,
                   ISNULL(clientify_inline_attempts, 0) AS clientify_inline_attempts
              FROM dbo.purchase_events
             WHERE ISNULL(clientify_inline_synced, 0) = 0
               AND ISNULL(clientify_inline_attempts, 0) < 10
             ORDER BY fecha ASC
            """,
                (rs, rowNum) -> new PendingInlineSyncRow(
                        rs.getString("purchase_id"),
                        rs.getString("telefono"),
                        rs.getString("payload_json"),
                        rs.getObject("clientify_contact_id", Long.class),
                        rs.getInt("clientify_inline_attempts")
                ),
                limit
        );
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record PendingInlineSyncRow(
            String purchaseId,
            String telefono,
            String payloadJson,
            Long clientifyContactId,
            int attempts
    ) {}
}