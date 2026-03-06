package com.batteryplus.survey.infra.service;

import com.batteryplus.survey.core.model.VerinaSaleRow;
import com.batteryplus.survey.infra.repository.CheckpointRepository;
import com.batteryplus.survey.infra.verina.VerinaSalesRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@ConditionalOnProperty(prefix="app.datasource.verina", name="enabled", havingValue="true")
@Service
public class VerinaPullService {

    private static final String SOURCE = "verina.tickets.acumulador";

    private final CheckpointRepository checkpointRepository;
    private final VerinaSalesRepository verinaSalesRepository;

    public VerinaPullService(CheckpointRepository checkpointRepository,
                             VerinaSalesRepository verinaSalesRepository) {
        this.checkpointRepository = checkpointRepository;
        this.verinaSalesRepository = verinaSalesRepository;
    }

    public List<VerinaSaleRow> pullNewSales(boolean updateCheckpoint) {
        // Si no hay checkpoint, arranca “desde lejos” para no traer todo.
        // Ajusta la fecha según tu necesidad.
        LocalDateTime from = checkpointRepository
                .getLastDateTime(SOURCE)
                .orElse(LocalDateTime.of(2026, 1, 1, 0, 0));

        List<VerinaSaleRow> rows = verinaSalesRepository.findSalesSince(from);

        if (updateCheckpoint && !rows.isEmpty()) {
            // Nos quedamos con la fecha máxima que regresó Verina (ya viene ORDER BY fecha asc, pero no confío)
            LocalDateTime maxFecha = rows.stream()
                    .map(VerinaSaleRow::fecha)
                    .max(LocalDateTime::compareTo)
                    .orElse(from);

            checkpointRepository.upsertLastDateTime(SOURCE, maxFecha);
        }

        return rows;
    }
}