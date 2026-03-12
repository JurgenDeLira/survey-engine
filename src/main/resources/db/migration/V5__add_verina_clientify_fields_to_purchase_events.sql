IF COL_LENGTH('dbo.purchase_events', 'me_gama') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD me_gama NVARCHAR(100) NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'me_marca_auto') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD me_marca_auto NVARCHAR(150) NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'me_modelo_auto') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD me_modelo_auto NVARCHAR(150) NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'me_anio_auto') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD me_anio_auto INT NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'me_fecha_fin_garantia') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD me_fecha_fin_garantia NVARCHAR(20) NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'pais') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD pais NVARCHAR(100) NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'estado_provincia') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD estado_provincia NVARCHAR(100) NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'ciudad') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD ciudad NVARCHAR(100) NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'origen') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD origen NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'estado') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD estado NVARCHAR(50) NULL;
END;
GO