package com.batteryplus.survey.connector.verina;

// SQL (SELECT incremental)
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
              AND vTickets.Fecha > ?
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
        SELECT TOP (?)
            t.IDSucursal,
            t.Sucursal,
            t.Numero AS Ticket,
            t.Fecha AS ME_Fecha_ultima_compra,

            CASE
                WHEN t.IDSucursal IN (1, 2, 3, 4, 5, 10, 12) THEN 'Los Mochis'
                WHEN t.IDSucursal IN (6, 7, 8, 9, 11, 13) THEN 'Culiacan'
                ELSE ''
            END AS Propietario,

            t.Subfamilia AS ME_Gama,
            t.Marca AS ME_Marca_bateria,
            t.Producto AS ME_Bateria_adquirida,

            g.Automovilista AS Nombre,
            CONCAT_WS(' ', g.Automovilista_PA, g.Automovilista_SA) AS Apellido,
            g.Telefono,
            g.Email AS Correo_electronico,

            g.Marca AS ME_Marca_auto,
            g.Modelo AS ME_Modelo_auto,
            TRY_CAST(g.Tipo AS INT) AS ME_Anio_auto,
            CONVERT(DATE, g.FechaFin) AS ME_Fecha_fin_garantia,

            'Mexico' AS Pais,
            '' AS Estado_Provincia,
            '' AS Ciudad,
            'Verina' AS Origen,
            'Activo' AS Estado

        FROM Tickets_numerados t
        LEFT JOIN Garantias_numeradas g
            ON t.Numero = g.IDTicket
           AND t.IDSucursal = g.IDSucursal
           AND t.rn = g.rn
        ORDER BY t.Fecha ASC, t.IDSucursal ASC, t.Numero ASC;
        """;
}