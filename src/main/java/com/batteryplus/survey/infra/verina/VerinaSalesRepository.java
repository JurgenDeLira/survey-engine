package com.batteryplus.survey.infra.verina;

import com.batteryplus.survey.core.model.VerinaSaleRow;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@ConditionalOnProperty(prefix="app.datasource.verina", name="enabled", havingValue="true")
@Repository
public class VerinaSalesRepository {

    private final JdbcTemplate verinaJdbc;

    public VerinaSalesRepository(@Qualifier("verinaJdbcTemplate") JdbcTemplate verinaJdbc) {
        this.verinaJdbc = verinaJdbc;
    }

    private static final RowMapper<VerinaSaleRow> ROW_MAPPER = (rs, rowNum) -> new VerinaSaleRow(
            rs.getString("Ticket"),
            rs.getInt("IDSucursal"),
            rs.getString("Sucursal"),
            rs.getTimestamp("Fecha").toLocalDateTime(),
            rs.getString("Producto"),
            rs.getInt("Cantidad"),
            rs.getString("Nombre_Automovilista"),
            rs.getString("Telefono"),
            rs.getString("Email")
    );

    /**
     * Trae ventas nuevas (incremental) a partir de un cursor de fecha.
     * OJO: Usamos ">" para no repetir el último registro.
     */
    public List<VerinaSaleRow> findSalesSince(LocalDateTime fromExclusive) {
        String sql = """
            DECLARE @from DATETIME2 = ?;

            WITH Tickets_numerados AS (
                SELECT
                    vTickets.IDSucursal,
                    vTickets.Sucursal,
                    vTickets.Numero,
                    vTickets.Fecha,
                    vTickets.Producto,
                    vTickets.Cantidad,
                    ROW_NUMBER() OVER (PARTITION BY vTickets.Numero, vTickets.IDSucursal ORDER BY (SELECT NULL)) AS rn
                FROM vTickets
                LEFT JOIN [vTickets/Cancelaciones]
                    ON vTickets.Numero = [vTickets/Cancelaciones].Numero
                    AND vTickets.IDSucursal = [vTickets/Cancelaciones].IDSucursal
                    AND vTickets.Familia = [vTickets/Cancelaciones].Familia
                LEFT JOIN [vTickets/Garantias]
                    ON vTickets.Numero = [vTickets/Garantias].Numero
                    AND vTickets.IDSucursal = [vTickets/Garantias].IDSucursal
                    AND vTickets.Familia = [vTickets/Garantias].Familia
                LEFT JOIN Ventas
                    ON vTickets.Numero = Ventas.IDTicket
                    AND vTickets.IDSucursal = Ventas.IDSucursal
                WHERE
                    [vTickets/Cancelaciones].Numero IS NULL
                    AND [vTickets/Garantias].Numero IS NULL
                    AND Ventas.IDTicket IS NOT NULL
                    AND vTickets.Fecha > @from
                    AND vTickets.Familia = 'ACUMULADOR'
                    AND vTickets.IDSucursal NOT IN (1,2,3,4,5,12)
            ),
            Garantias_numeradas AS (
                SELECT
                    [Garantias/Expedicion].IDTicket,
                    [Garantias/Expedicion].IDSucursal,
                    [Garantias/Expedicion].Automovilista,
                    [Garantias/Expedicion].Automovilista_PA,
                    [Garantias/Expedicion].Automovilista_SA,
                    [Garantias/Expedicion].Telefono,
                    [Garantias/Expedicion].Email,
                    ROW_NUMBER() OVER (PARTITION BY [Garantias/Expedicion].IDTicket, [Garantias/Expedicion].IDSucursal ORDER BY (SELECT NULL)) AS rn
                FROM [Garantias/Expedicion]
            )
            SELECT
                t.IDSucursal,
                t.Sucursal,
                t.Numero AS Ticket,
                t.Fecha,
                t.Producto,
                t.Cantidad,
                CONCAT_WS(' ', g.Automovilista, g.Automovilista_PA, g.Automovilista_SA) AS Nombre_Automovilista,
                g.Telefono,
                g.Email
            FROM Tickets_numerados t
            LEFT JOIN Garantias_numeradas g
                ON t.Numero = g.IDTicket
                AND t.IDSucursal = g.IDSucursal
                AND t.rn = g.rn
            ORDER BY t.Fecha ASC;
            """;

        return verinaJdbc.query(sql, ROW_MAPPER, Timestamp.valueOf(fromExclusive));
    }
}