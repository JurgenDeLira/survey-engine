package com.batteryplus.survey.connector.verina;

//SQL (SELECT incremental)
public class VerinaQueries {
    private VerinaQueries() {}

    public static final String FETCH_AFTER_DATE = """
        WITH Tickets_numerados AS (
            SELECT vTickets.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY vTickets.Numero, vTickets.IDSucursal
                       ORDER BY (SELECT NULL)
                   ) AS rn
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
            WHERE [vTickets/Cancelaciones].Numero IS NULL
              AND [vTickets/Garantias].Numero IS NULL
              AND Ventas.IDTicket IS NOT NULL

              -- incremental por fecha (checkpoint)
              AND vTickets.Fecha > ?

              -- filtro negocio (MVP)
              AND vTickets.Familia = 'ACUMULADOR'
              AND vTickets.IDSucursal NOT IN (1,2,3,4,5,12)
        ),
        Garantias_numeradas AS (
            SELECT [Garantias/Expedicion].*,
                   ROW_NUMBER() OVER (
                       PARTITION BY [Garantias/Expedicion].IDTicket, [Garantias/Expedicion].IDSucursal
                       ORDER BY (SELECT NULL)
                   ) AS rn
            FROM [Garantias/Expedicion]
        )
        SELECT
            t.IDSucursal,
            t.Sucursal,
            t.Numero AS Ticket,
            t.Fecha,
            t.Familia,
            t.Marca,
            t.IDProducto,
            t.BCI,
            t.Producto,
            t.Cantidad,

            CONCAT_WS(' ', g.Automovilista, g.Automovilista_PA, g.Automovilista_SA) AS Nombre_Automovilista,
            g.Telefono,
            g.Email,
            g.Marca AS Marca_Carro,
            g.Tipo,
            g.Modelo,

            CONVERT(DATE, g.FechaFin) AS Fecha_Garantia,
            g.NoGarantia

        FROM Tickets_numerados t
        LEFT JOIN Garantias_numeradas g
            ON t.Numero = g.IDTicket
           AND t.IDSucursal = g.IDSucursal
           AND t.rn = g.rn
        ORDER BY t.Fecha ASC;
        """;
}