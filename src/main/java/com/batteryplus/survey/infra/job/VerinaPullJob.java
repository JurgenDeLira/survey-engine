package com.batteryplus.survey.infra.job;

//@Scheduled: pull incremental

import com.batteryplus.survey.connector.verina.SaleRow;
import com.batteryplus.survey.connector.verina.VerinaPurchaseReader;
import com.batteryplus.survey.infra.repository.CheckpointRepository;
import com.batteryplus.survey.infra.repository.PurchaseEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.LocalDateTime;
import java.util.List;

//LA SIGUIENTE LINEA ES PARA HACER PRUEBA SIN STAGING
@ConditionalOnProperty(name="app.jobs.verinaPull.enabled", havingValue="true")
@Component
public class VerinaPullJob {

    private static final String SOURCE = "VERINA";

    private final CheckpointRepository checkpointRepository;
    private final PurchaseEventRepository purchaseEventRepository;
    private final VerinaPurchaseReader verinaPurchaseReader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VerinaPullJob(
            CheckpointRepository checkpointRepository,
            PurchaseEventRepository purchaseEventRepository,
            VerinaPurchaseReader verinaPurchaseReader
    ) {
        this.checkpointRepository = checkpointRepository;
        this.purchaseEventRepository = purchaseEventRepository;
        this.verinaPurchaseReader = verinaPurchaseReader;
    }

    // cada 5 minutos
    @Scheduled(fixedDelay = 300_000)
    public void pullFromVerina() throws Exception {

        // 1) Tomar checkpoint por fecha. En primera corrida, leer una ventana (ej. 15 min).
        LocalDateTime lastDateTime = checkpointRepository
                .getLastDateTime(SOURCE)
                .orElse(LocalDateTime.now().minusMinutes(15));

        // 2) Leer ventas nuevas desde tu query (WHERE vTickets.Fecha > ?)
        List<SaleRow> rows = verinaPurchaseReader.fetchAfter(lastDateTime);

        LocalDateTime maxFecha = lastDateTime;

        for (SaleRow row : rows) {

            // Seguridad básica: si Fecha viene null, saltar
            if (row.fecha() == null) continue;

            // 3) purchaseId (candado anti-duplicados)
            String purchaseId = "VERINA-" + row.idSucursal() + "-" + row.ticket();

            // 4) payload para auditoría (opcional)
            String payloadJson = objectMapper.writeValueAsString(row);

            // 5) Insert idempotente en staging
            boolean inserted = purchaseEventRepository.insertIfNotExists(
                    purchaseId,
                    SOURCE,
                    row.fecha(),
                    row.idSucursal(),
                    row.ticket(),
                    row.telefono(),
                    row.nombreAutomovilista(),
                    row.email(),
                    row.familia(),
                    row.marca(),
                    row.producto(),
                    row.cantidad(),
                    payloadJson
            );

            // (Por ahora) solo guardamos; luego aquí metemos Clientify si inserted==true
            // if (inserted) { ...llamar a Clientify... }

            // 6) Trackear max fecha procesada para actualizar checkpoint
            if (row.fecha().isAfter(maxFecha)) {
                maxFecha = row.fecha();
            }
        }

        // 7) Actualizar checkpoint al final
        if (maxFecha.isAfter(lastDateTime)) {
            checkpointRepository.upsertLastDateTime(SOURCE, maxFecha);
        }
    }
}