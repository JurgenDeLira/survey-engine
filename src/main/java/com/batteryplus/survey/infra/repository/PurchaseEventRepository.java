package com.batteryplus.survey.infra.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

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
                    payload_json
                ) VALUES (?, ?, ?, ?, ?, ?,
                         ?, ?, ?, ?,
                         ?, ?, ?, ?,
                         ?, ?, ?, ?, ?,
                         ?, ?, ?, ?, ?,
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
                    payloadJson
            );
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }
}