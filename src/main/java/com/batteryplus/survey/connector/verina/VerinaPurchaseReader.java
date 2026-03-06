package com.batteryplus.survey.connector.verina;

//lee de Verina (JdbcTemplate)

import com.batteryplus.survey.core.model.VerinaTicketRow;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static com.batteryplus.survey.connector.verina.VerinaQueries.FETCH_AFTER_DATE;
import static com.batteryplus.survey.connector.verina.VerinaRowMappers.VERINA_TICKET_ROW;

@ConditionalOnProperty(name = "app.datasource.verina.enabled", havingValue = "true")
@Component
public class VerinaPurchaseReader {

    private final JdbcTemplate verinaJdbc;

    public VerinaPurchaseReader(@Qualifier("verinaJdbcTemplate") JdbcTemplate verinaJdbc) {
        this.verinaJdbc = verinaJdbc;
    }

    public List<VerinaTicketRow> fetchAfter(LocalDateTime lastDateTime, int limit) {
        if (lastDateTime == null) throw new IllegalArgumentException("lastDateTime cannot be null");
        if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");

        return verinaJdbc.query(
                FETCH_AFTER_DATE,
                VERINA_TICKET_ROW,
                Timestamp.valueOf(lastDateTime),
                limit
        );
    }
}