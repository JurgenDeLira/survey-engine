package com.batteryplus.survey.infra.service;

import com.batteryplus.survey.adapter.clientify.ClientifyService;
import com.batteryplus.survey.connector.verina.VerinaPurchaseReader;
import com.batteryplus.survey.core.model.VerinaTicketRow;
import com.batteryplus.survey.core.normalize.PhoneNormalizer;
import com.batteryplus.survey.infra.repository.CheckpointRepository;
import com.batteryplus.survey.infra.repository.PurchaseEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.datasource.verina.enabled", havingValue = "true")
public class VerinaPullService {

    private static final Logger log = LoggerFactory.getLogger(VerinaPullService.class);
    private static final String SOURCE = "VERINA_ACUMULADOR";
    private static final int FETCH_LIMIT = 200;

    private final CheckpointRepository checkpointRepository;
    private final PurchaseEventRepository purchaseEventRepository;
    private final VerinaPurchaseReader verinaPurchaseReader;
    private final ClientifyService clientifyService;
    private final PhoneNormalizer phoneNormalizer;
    private final ObjectMapper objectMapper;

    public VerinaPullService(
            CheckpointRepository checkpointRepository,
            PurchaseEventRepository purchaseEventRepository,
            VerinaPurchaseReader verinaPurchaseReader,
            ClientifyService clientifyService,
            PhoneNormalizer phoneNormalizer,
            ObjectMapper objectMapper
    ) {
        this.checkpointRepository = checkpointRepository;
        this.purchaseEventRepository = purchaseEventRepository;
        this.verinaPurchaseReader = verinaPurchaseReader;
        this.clientifyService = clientifyService;
        this.phoneNormalizer = phoneNormalizer;
        this.objectMapper = objectMapper;
    }

    public int runOnce() throws Exception {
        LocalDateTime lastDateTime = checkpointRepository
                .getLastDateTime(SOURCE)
                .orElse(LocalDateTime.now().minusDays(2));

        List<VerinaTicketRow> rows = verinaPurchaseReader.fetchAfter(lastDateTime, FETCH_LIMIT);

        LocalDateTime maxFecha = lastDateTime;
        int processed = 0;
        int duplicates = 0;
        int invalidPhone = 0;

        log.info("Iniciando pull Verina. source={} lastDateTime={} fetched={}", SOURCE, lastDateTime, rows.size());

        for (VerinaTicketRow row : rows) {
            if (row.fechaUltimaCompra() == null) {
                continue;
            }

            String purchaseId = "VERINA-" + row.idSucursal() + "-" + row.ticket();
            String payloadJson = objectMapper.writeValueAsString(row);

            boolean inserted = purchaseEventRepository.insertIfNotExists(
                    purchaseId,
                    SOURCE,
                    row.fechaUltimaCompra(),
                    row.idSucursal(),
                    row.me14Sucursal(),
                    row.ticket(),
                    row.telefono(),
                    buildNombreCompleto(row),
                    row.correoElectronico(),
                    row.propietario(),
                    null,
                    row.meMarcaBateria(),
                    row.meBateriaAdquirida(),
                    null,
                    row.meGama(),
                    row.meMarcaAuto(),
                    row.meModeloAuto(),
                    row.meAnioAuto(),
                    row.meFechaFinGarantia(),
                    row.pais(),
                    row.estadoProvincia(),
                    row.ciudad(),
                    row.origen(),
                    row.estado(),
                    payloadJson
            );

            if (inserted) {
                processed++;

                String phoneE164 = phoneNormalizer.toE164OrNull(row.telefono());

                if (phoneE164 == null) {
                    invalidPhone++;
                    log.warn("Venta insertada pero sin teléfono válido. purchaseId={} tel={}", purchaseId, row.telefono());
                } else {
                    String ticketValue = row.meUltimaCompraTicket();

                    try {
                        boolean ok = clientifyService.upsertContactFromSale(
                                phoneE164,
                                ticketValue,
                                row
                        );
                        if (!ok) {
                            log.warn("No se pudo crear/actualizar contacto en Clientify. phone={} purchaseId={}", phoneE164, purchaseId);
                        }
                    } catch (Exception ex) {
                        log.error("Error actualizando Clientify. purchaseId={} phone={}", purchaseId, phoneE164, ex);
                    }
                }
            } else {
                duplicates++;
            }

            if (row.fechaUltimaCompra().isAfter(maxFecha)) {
                maxFecha = row.fechaUltimaCompra();
            }
        }

        if (maxFecha.isAfter(lastDateTime)) {
            checkpointRepository.upsertLastDateTime(SOURCE, maxFecha);
            log.info("Checkpoint actualizado. source={} newLastDateTime={}", SOURCE, maxFecha);
        }

        log.info(
                "Pull Verina finalizado. source={} fetched={} inserted={} duplicates={} invalidPhone={}",
                SOURCE, rows.size(), processed, duplicates, invalidPhone
        );

        return processed;
    }

    private String buildNombreCompleto(VerinaTicketRow row) {
        String nombre = row.nombre() == null ? "" : row.nombre().trim();
        String apellido = row.apellido() == null ? "" : row.apellido().trim();
        return (nombre + " " + apellido).trim();
    }
}