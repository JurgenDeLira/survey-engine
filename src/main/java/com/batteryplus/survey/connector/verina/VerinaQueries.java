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
                WHEN t.IDSucursal IN (6, 7, 8, 9, 11, 13) THEN 'MARTIN S.'
                ELSE 'Daniela Cota'
            END AS Propietario,

            CASE
                WHEN t.Marca IN ('LTH') THEN 'ESTANDAR'
                WHEN t.Marca IN ('HEAVY DUTY', 'LTH ESPECIALES') THEN 'ESPECIALES'
                WHEN t.Marca IN ('PROTECT', 'OPTIMA', 'LTH HI-TEC', 'OTROS', 'LTH AGM') THEN 'PREMIUM'
                WHEN t.Marca IN ('POWER CONNECTION', 'DIENER') THEN 'SUPER VALUE'
                WHEN t.Marca IN ('AMERICA', 'CRONOS') THEN 'VALUE'
                ELSE 'SIN CLASIFICAR'
            END AS ME_Gama,

            t.Marca AS ME_Marca_bateria,

            CASE
                WHEN UPPER(t.Producto) LIKE '%TROJAN%' THEN
                    LTRIM(RTRIM(
                        SUBSTRING(
                            t.Producto,
                            CHARINDEX('TROJAN', UPPER(t.Producto)),
                            LEN(t.Producto)
                        )
                    ))
                WHEN CHARINDEX('BATERIA', UPPER(t.Producto)) > 0 THEN
                    LTRIM(RTRIM(
                        LEFT(
                            t.Producto,
                            CHARINDEX('BATERIA', UPPER(t.Producto)) - 1
                        )
                    ))
                WHEN CHARINDEX('ACUMULADOR', UPPER(t.Producto)) > 0 THEN
                    LTRIM(RTRIM(
                        LEFT(
                            t.Producto,
                            CHARINDEX('ACUMULADOR', UPPER(t.Producto)) - 1
                        )
                    ))
                ELSE LEFT(t.Producto, CHARINDEX(' ', t.Producto + ' ') - 1)
            END AS ME_Bateria_adquirida,

            g.Automovilista AS Nombre,
            CONCAT_WS(' ', g.Automovilista_PA, g.Automovilista_SA) AS Apellido,
            g.Telefono AS Telefono,
            LOWER(g.Email) AS Correo_electronico,

            g.Marca AS ME_Marca_auto,
            g.Tipo AS ME_Modelo_auto,
            TRY_CAST(g.Modelo AS INT) AS ME_Anio_auto,
            CONVERT(VARCHAR(10), g.FechaFin, 23) AS ME_Fecha_fin_garantia,

            'México' AS Pais,
            'Sinaloa' AS Estado_Provincia,

            CASE
                WHEN t.IDSucursal IN (1, 2, 3, 4, 5, 10, 12) THEN 'Los Mochis'
                WHEN t.IDSucursal IN (6, 7, 8, 9, 11, 13) THEN 'Culiacán'
                ELSE ''
            END AS Ciudad,

            CASE
                WHEN t.Sucursal LIKE '%SERVICIO DOMICILIO%' THEN 'Domicilio'
                ELSE 'Piso'
            END AS Origen,

            CASE
                WHEN t.IDSucursal IN (6, 7, 8, 9, 11, 13) THEN CONCAT('CLN- ', t.Sucursal)
                ELSE CONCAT('LM- ', t.Sucursal)
            END AS Sucursal_Formateada,

            'venta' AS Estado

        FROM Tickets_numerados t
        LEFT JOIN Garantias_numeradas g
            ON t.Numero = g.IDTicket
           AND t.IDSucursal = g.IDSucursal
           AND t.rn = g.rn
        ORDER BY t.Fecha ASC, t.IDSucursal ASC, t.Numero ASC;
        """;
}