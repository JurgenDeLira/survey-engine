package com.batteryplus.survey.infra.job;

import com.batteryplus.survey.adapter.clientify.ClientifyService;
import com.batteryplus.survey.core.model.VerinaTicketRow;
import com.batteryplus.survey.core.normalize.PhoneNormalizer;
import com.batteryplus.survey.infra.repository.PurchaseEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientifyInlineRetryJob {

    private static final Logger log = LoggerFactory.getLogger(ClientifyInlineRetryJob.class);
    private static final int BATCH_SIZE = 50;

    private final PurchaseEventRepository purchaseEventRepository;
    private final ClientifyService clientifyService;
    private final ObjectMapper objectMapper;
    private final PhoneNormalizer phoneNormalizer;

    public ClientifyInlineRetryJob(
            PurchaseEventRepository purchaseEventRepository,
            ClientifyService clientifyService,
            ObjectMapper objectMapper,
            PhoneNormalizer phoneNormalizer
    ) {
        this.purchaseEventRepository = purchaseEventRepository;
        this.clientifyService = clientifyService;
        this.objectMapper = objectMapper;
        this.phoneNormalizer = phoneNormalizer;
    }

    @Scheduled(initialDelay = 120_000, fixedDelay = 600_000)
    public void retryPendingInlineSync() {
        List<PurchaseEventRepository.PendingInlineSyncRow> pending =
                purchaseEventRepository.findPendingInlineSync(BATCH_SIZE);

        if (pending.isEmpty()) {
            log.info("ClientifyInlineRetryJob sin pendientes.");
            return;
        }

        int synced = 0;
        int failed = 0;

        log.info("ClientifyInlineRetryJob iniciando. pending={}", pending.size());

        for (PurchaseEventRepository.PendingInlineSyncRow row : pending) {
            try {
                VerinaTicketRow payload = objectMapper.readValue(row.payloadJson(), VerinaTicketRow.class);

                Long contactId = row.clientifyContactId();
                if (contactId == null) {
                    String phoneE164 = phoneNormalizer.toE164OrNull(row.telefono());
                    contactId = clientifyService.resolveExistingContactIdByPhone(phoneE164);
                }

                if (contactId == null) {
                    failed++;
                    purchaseEventRepository.markClientifyInlineSyncFailed(
                            row.purchaseId(),
                            null,
                            "Retry inline: no se encontró contactId"
                    );
                    continue;
                }

                ClientifyService.InlineSyncResult result =
                        clientifyService.retryInlineSync(contactId, payload);

                if (result.success()) {
                    synced++;
                    purchaseEventRepository.markClientifyInlineSyncSuccess(row.purchaseId(), contactId);
                } else {
                    failed++;
                    purchaseEventRepository.markClientifyInlineSyncFailed(
                            row.purchaseId(),
                            contactId,
                            result.errorMessage()
                    );
                }
            } catch (Exception ex) {
                failed++;
                log.error("Error reprocesando inline. purchaseId={}", row.purchaseId(), ex);
                purchaseEventRepository.markClientifyInlineSyncFailed(
                        row.purchaseId(),
                        row.clientifyContactId(),
                        ex.getMessage()
                );
            }
        }

        log.info(
                "ClientifyInlineRetryJob finalizado. pending={} synced={} failed={}",
                pending.size(),
                synced,
                failed
        );
    }
}