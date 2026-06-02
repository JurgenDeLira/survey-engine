IF COL_LENGTH('dbo.purchase_events', 'clientify_created_new_contact') IS NULL
BEGIN
ALTER TABLE dbo.purchase_events
    ADD clientify_created_new_contact BIT NULL;
END;
GO