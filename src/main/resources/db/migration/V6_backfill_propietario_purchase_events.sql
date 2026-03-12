UPDATE dbo.purchase_events
SET propietario = CASE
                      WHEN id_sucursal IN (1, 2, 3, 4, 5, 10, 12) THEN 'Los Mochis'
                      WHEN id_sucursal IN (6, 7, 8, 9, 11, 13) THEN 'Culiacan'
                      ELSE propietario
    END
WHERE propietario IS NULL
  AND id_sucursal IS NOT NULL;
GO