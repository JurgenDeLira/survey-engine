package com.batteryplus.survey.infra.repository;

//lee/guarda checkpoint

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CheckpointRepository {

    private final JdbcTemplate stagingJdbc;

    public CheckpointRepository(JdbcTemplate stagingJdbcTemplate) {
        this.stagingJdbc = stagingJdbcTemplate;
    }

    public Optional<Long> getLastTicketId(String source) {
        return stagingJdbc.query(
                "SELECT last_ticket_id FROM dbo.checkpoints WHERE source = ?",
                rs -> rs.next() ? Optional.ofNullable(rs.getLong(1)) : Optional.empty(),
                source
        );
    }

    public void upsertlastTicketId(String source, long lastTicketId) {
        //Aqui hago el merge para SQL server
        stagingJdbc.update("""
                MERGE dbo.checkpoints AS target
                USING (SELECT ? AS source, ? AS last_ticket_id) AS src
                ON target.source = src.source
                WHEN MATCHED THEN
                    UPDATE SET last_ticket_id = src.last_ticket_id, updated_at = SYSUTCDATETIME()
                WHEN NOT MATCHED THEN
                    INSERT (source, last_ticket_id) VALUES (src.source, src.last_ticket_id);
                """, source, lastTicketId);
    }
}
