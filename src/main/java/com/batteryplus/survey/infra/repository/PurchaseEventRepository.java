package com.batteryplus.survey.infra.repository;

//inserta/consulta eventos

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@ConditionalOnProperty(prefix="app.datasource.staging", name="enabled", havingValue="true")
@Repository
public class PurchaseEventRepository {

    private final JdbcTemplate stagingJdbc;

    public PurchaseEventRepository(@Qualifier("stagingJdbcTemplate") JdbcTemplate stagingJdbc) {
        this.stagingJdbc = stagingJdbc;
    }

    /**
     * Inserta una venta en staging solo si no existe.
     * @return true si insertó (venta nueva), false si ya existía (ya procesada)
     */
    public boolean insertIfNotExists(
            String purchaseId,
            String source,
            LocalDateTime fecha,
            int idSucursal,
            long ticket,
            String telefono,
            String nombre,
            String email,
            String familia,
            String marca,
            String producto,
            Integer cantidad,
            String payloadJson
    ) {
        try {
            stagingJdbc.update("""
                INSERT INTO dbo.purchase_events (
                    purchase_id, source, fecha, id_sucursal, ticket,
                    telefono, nombre, email,
                    familia, marca, producto, cantidad,
                    payload_json
                ) VALUES (?, ?, ?, ?, ?,
                         ?, ?, ?,
                         ?, ?, ?, ?,
                         ?)
                """,
                    purchaseId,
                    source,
                    Timestamp.valueOf(fecha),
                    idSucursal,
                    ticket,
                    telefono,
                    nombre,
                    email,
                    familia,
                    marca,
                    producto,
                    cantidad,
                    payloadJson
            );
            return true;
        } catch (DuplicateKeyException ex) {
            // Ya existía purchase_id (o el unique constraint source+id_sucursal+ticket)
            return false;
        }
    }
}