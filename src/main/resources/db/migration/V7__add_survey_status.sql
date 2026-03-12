ALTER TABLE dbo.purchase_events
    ADD survey_status VARCHAR(20) NULL;
GO

UPDATE dbo.purchase_events
SET survey_status = 'pending'
WHERE survey_status IS NULL;
GO