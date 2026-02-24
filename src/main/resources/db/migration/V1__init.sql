/* =========================================================
   V1__init.sql  (SQL Server)
   Tablas:
   - checkpoints
   - purchase_events
   ========================================================= */

-- =========================
-- 1) CHECKPOINTS
-- Guarda hasta qué fecha ya procesaste por fuente (VERINA, etc.)
-- =========================
IF OBJECT_ID('dbo.checkpoints', 'U') IS NULL
BEGIN
CREATE TABLE dbo.checkpoints (
                                 source        NVARCHAR(50)  NOT NULL,
                                 last_datetime DATETIME2(0)   NULL,
                                 updated_at    DATETIME2(0)   NOT NULL CONSTRAINT DF_checkpoints_updated_at DEFAULT SYSUTCDATETIME(),
                                 CONSTRAINT PK_checkpoints PRIMARY KEY (source)
);
END;
GO

-- =========================
-- 2) PURCHASE_EVENTS (staging)
-- Aquí guardas ventas detectadas desde el query (idempotente)
-- =========================
IF OBJECT_ID('dbo.purchase_events', 'U') IS NULL
BEGIN
CREATE TABLE dbo.purchase_events (
                                     purchase_id    NVARCHAR(100) NOT NULL,  -- "VERINA-{IDSucursal}-{Ticket}"
                                     source         NVARCHAR(50)  NOT NULL,  -- "VERINA"
                                     fecha          DATETIME2(0)   NOT NULL,  -- fecha de venta (q.Fecha)

                                     id_sucursal    INT           NOT NULL,
                                     ticket         BIGINT        NOT NULL,

                                     telefono       NVARCHAR(50)  NULL,
                                     nombre         NVARCHAR(200) NULL,
                                     email          NVARCHAR(200) NULL,

                                     familia        NVARCHAR(100) NULL,
                                     marca          NVARCHAR(200) NULL,
                                     producto       NVARCHAR(300) NULL,
                                     cantidad       INT           NULL,

                                     payload_json   NVARCHAR(MAX) NULL,      -- útil para auditoría
                                     created_at     DATETIME2(0)  NOT NULL CONSTRAINT DF_purchase_events_created_at DEFAULT SYSUTCDATETIME(),

                                     CONSTRAINT PK_purchase_events PRIMARY KEY (purchase_id),
                                     CONSTRAINT UQ_purchase_events_source_ticket UNIQUE (source, id_sucursal, ticket)
);
END;
GO

CREATE INDEX IX_purchase_events_fecha
    ON dbo.purchase_events (fecha DESC);
GO

CREATE INDEX IX_purchase_events_telefono
    ON dbo.purchase_events (telefono);
GO

