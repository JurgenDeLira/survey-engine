IF COL_LENGTH('dbo.purchase_events', 'clientify_contact_id') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD clientify_contact_id BIGINT NULL;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'clientify_inline_synced') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD clientify_inline_synced BIT NOT NULL
    CONSTRAINT DF_purchase_events_clientify_inline_synced DEFAULT 0;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'clientify_inline_attempts') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD clientify_inline_attempts INT NOT NULL
    CONSTRAINT DF_purchase_events_clientify_inline_attempts DEFAULT 0;
END;
GO

IF COL_LENGTH('dbo.purchase_events', 'clientify_inline_last_error') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD clientify_inline_last_error NVARCHAR(1000) NULL;
END;
GO