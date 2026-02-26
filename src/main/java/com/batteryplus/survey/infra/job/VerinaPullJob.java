package com.batteryplus.survey.infra.job;

//@Scheduled: pull incremental

import com.batteryplus.survey.adapter.clientify.ClientifyService;
import com.batteryplus.survey.connector.verina.SaleRow;
import com.batteryplus.survey.connector.verina.VerinaPurchaseReader;
import com.batteryplus.survey.core.normalize.PhoneNormalizer;
import com.batteryplus.survey.infra.repository.CheckpointRepository;
import com.batteryplus.survey.infra.repository.PurchaseEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@ConditionalOnProperty(name="app.jobs.verinaPull.enabled", havingValue="true")
@Component
public class VerinaPullJob {

    private static final String SOURCE = "VERINA";

    private final CheckpointRepository checkpointRepository;
    private final PurchaseEventRepository purchaseEventRepository;
    private final VerinaPurchaseReader verinaPurchaseReader;
    private final ClientifyService clientifyService;
    private final PhoneNormalizer phoneNormalizer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VerinaPullJob(
            CheckpointRepository checkpointRepository,
            PurchaseEventRepository purchaseEventRepository,
            VerinaPurchaseReader verinaPurchaseReader,
            ClientifyService clientifyService,
            PhoneNormalizer phoneNormalizer
    ) {
        this.checkpointRepository = checkpointRepository;
        this.purchaseEventRepository = purchaseEventRepository;
        this.verinaPurchaseReader = verinaPurchaseReader;
        this.clientifyService = clientifyService;
        this.phoneNormalizer = phoneNormalizer;
    }

    @Scheduled(fixedDelay = 300_000)
    public void pullFromVerina() throws Exception {

        LocalDateTime lastDateTime = checkpointRepository
                .getLastDateTime(SOURCE)
                .orElse(LocalDateTime.now().minusMinutes(15));

        List<SaleRow> rows = verinaPurchaseReader.fetchAfter(lastDateTime);

        LocalDateTime maxFecha = lastDateTime;

        for (SaleRow row : rows) {
            if (row.fecha() == null) continue;

            String purchaseId = "VERINA-" + row.idSucursal() + "-" + row.ticket();
            String payloadJson = objectMapper.writeValueAsString(row);

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

            if (inserted) {
                // Normaliza a E164 (+521...)
                String phoneE164 = phoneNormalizer.toE164OrNull(row.telefono());
                if (phoneE164 != null) {
                    clientifyService.upsertUltimaCompraTicketAndTagByPhone(
                            phoneE164,
                            String.valueOf(row.ticket())
                    );
                }
            }

            if (row.fecha().isAfter(maxFecha)) maxFecha = row.fecha();
        }

        if (maxFecha.isAfter(lastDateTime)) {
            checkpointRepository.upsertLastDateTime(SOURCE, maxFecha);
        }
    }
}