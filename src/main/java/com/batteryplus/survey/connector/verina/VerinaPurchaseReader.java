package com.batteryplus.survey.connector.verina;

//lee de Verina (JdbcTemplate)

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static com.batteryplus.survey.connector.verina.VerinaQueries.FETCH_AFTER_DATE;
import static com.batteryplus.survey.connector.verina.VerinaRowMappers.SALE_ROW;

@Component
public class VerinaPurchaseReader {

    private final JdbcTemplate verinaJdbc;

    public VerinaPurchaseReader(@Qualifier("verinaJdbcTemplate") JdbcTemplate verinaJdbc) {
        this.verinaJdbc = verinaJdbc;
    }

    public List<SaleRow> fetchAfter(LocalDateTime lastDateTime) {
        if (lastDateTime == null) throw new IllegalArgumentException("lastDateTime cannot be null");

        return verinaJdbc.query(
                FETCH_AFTER_DATE,
                SALE_ROW,
                Timestamp.valueOf(lastDateTime)
        );
    }
}