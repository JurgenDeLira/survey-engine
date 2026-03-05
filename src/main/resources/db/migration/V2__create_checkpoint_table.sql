CREATE TABLE dbo.survey_checkpoint (
                                       id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                       ticket_id NVARCHAR(80) NOT NULL,
                                       created_at DATETIME2 NOT NULL CONSTRAINT df_survey_checkpoint_created_at DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uq_survey_checkpoint_ticket_id ON dbo.survey_checkpoint(ticket_id);