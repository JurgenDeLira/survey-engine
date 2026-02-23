-- =========================
-- 1) Checkpoints
-- =========================
IF OBJECT_ID('dbo.checkpoints', 'U') IS NULL
BEGIN
CREATE TABLE dbo.checkpoints (
                                 source            NVARCHAR(50)  NOT NULL,
                                 last_ticket_id     BIGINT        NULL,
                                 last_datetime      DATETIME2     NULL,
                                 updated_at         DATETIME2     NOT NULL CONSTRAINT DF_checkpoints_updated_at DEFAULT SYSUTCDATETIME(),
                                 CONSTRAINT PK_checkpoints PRIMARY KEY (source)
);
END
GO

-- =========================
-- 2) Purchase events
-- =========================
IF OBJECT_ID('dbo.purchase_events', 'U') IS NULL
BEGIN
CREATE TABLE dbo.purchase_events (
                                     purchase_id        NVARCHAR(100) NOT NULL,   -- "VERINA-<branch>-<ticket>"
                                     source             NVARCHAR(50)  NOT NULL,   -- "VERINA"
                                     ticket_id          BIGINT        NOT NULL,
                                     branch_id          INT           NOT NULL,
                                     branch_name        NVARCHAR(200) NULL,
                                     purchase_datetime  DATETIME2     NOT NULL,

                                     customer_key       NVARCHAR(200) NULL,       -- TEL:<normalized> o MAIL:<normalized>
                                     customer_name      NVARCHAR(200) NULL,
                                     customer_phone     NVARCHAR(50)  NULL,
                                     customer_email     NVARCHAR(200) NULL,

                                     payload_json       NVARCHAR(MAX) NULL,       -- opcional: auditoría (items, etc.)
                                     created_at         DATETIME2     NOT NULL CONSTRAINT DF_purchase_events_created_at DEFAULT SYSUTCDATETIME(),

                                     CONSTRAINT PK_purchase_events PRIMARY KEY (purchase_id),
                                     CONSTRAINT UQ_purchase_events_source_ticket UNIQUE (source, ticket_id, branch_id)
);
END
GO

CREATE INDEX IX_purchase_events_purchase_datetime
    ON dbo.purchase_events (purchase_datetime DESC);
GO

CREATE INDEX IX_purchase_events_customer_key
    ON dbo.purchase_events (customer_key);
GO

-- =========================
-- 3) Survey dispatch (control anti-spam + estado)
-- =========================
IF OBJECT_ID('dbo.survey_dispatch', 'U') IS NULL
BEGIN
CREATE TABLE dbo.survey_dispatch (
                                     id                BIGINT IDENTITY(1,1) NOT NULL,
                                     purchase_id        NVARCHAR(100) NOT NULL,
                                     customer_key       NVARCHAR(200) NOT NULL,
                                     survey_type        NVARCHAR(50)  NOT NULL,     -- ej: SATISFACCION_COMPRA

                                     status             NVARCHAR(20)  NOT NULL,     -- PENDING | SENT | FAILED | SKIPPED
                                     attempts           INT           NOT NULL CONSTRAINT DF_survey_dispatch_attempts DEFAULT 0,
                                     last_error         NVARCHAR(1000) NULL,

                                     created_at         DATETIME2     NOT NULL CONSTRAINT DF_survey_dispatch_created_at DEFAULT SYSUTCDATETIME(),
                                     sent_at            DATETIME2     NULL,

                                     CONSTRAINT PK_survey_dispatch PRIMARY KEY (id),
                                     CONSTRAINT FK_survey_dispatch_purchase_id FOREIGN KEY (purchase_id) REFERENCES dbo.purchase_events(purchase_id)
);
END
GO

CREATE INDEX IX_survey_dispatch_status
    ON dbo.survey_dispatch (status, created_at);
GO

CREATE INDEX IX_survey_dispatch_customer_key_type
    ON dbo.survey_dispatch (customer_key, survey_type, created_at DESC);
GO