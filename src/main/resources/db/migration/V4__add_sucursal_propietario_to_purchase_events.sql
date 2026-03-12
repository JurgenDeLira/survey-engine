IF COL_LENGTH('dbo.purchase_events', 'sucursal') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD sucursal NVARCHAR(150) NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'propietario') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD propietario NVARCHAR(100) NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_purchase_events_propietario'
      AND object_id = OBJECT_ID('dbo.purchase_events')
)
BEGIN
CREATE INDEX IX_purchase_events_propietario
    ON dbo.purchase_events (propietario);
END;
GO