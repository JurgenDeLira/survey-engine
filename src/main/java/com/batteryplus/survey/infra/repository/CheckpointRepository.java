package com.batteryplus.survey.infra.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class CheckpointRepository {

    private final JdbcTemplate stagingJdbc;

    public CheckpointRepository(@Qualifier("stagingJdbcTemplate") JdbcTemplate stagingJdbc) {
        this.stagingJdbc = stagingJdbc;
    }

    public Optional<LocalDateTime> getLastDateTime(String source) {
        return stagingJdbc.query(
                "SELECT last_datetime FROM dbo.checkpoints WHERE source = ?",
                rs -> {
                    if (!rs.next()) return Optional.empty();
                    Timestamp ts = rs.getTimestamp(1);
                    return ts == null ? Optional.empty() : Optional.of(ts.toLocalDateTime());
                },
                source
        );
    }

    public void upsertLastDateTime(String source, LocalDateTime lastDateTime) {
        stagingJdbc.update("""
            MERGE dbo.checkpoints AS target
            USING (SELECT ? AS source, ? AS last_datetime) AS src
            ON target.source = src.source
            WHEN MATCHED THEN
                UPDATE SET last_datetime = src.last_datetime, updated_at = SYSUTCDATETIME()
            WHEN NOT MATCHED THEN
                INSERT (source, last_datetime) VALUES (src.source, src.last_datetime);
            """,
                source,
                Timestamp.valueOf(lastDateTime)
        );
    }
}