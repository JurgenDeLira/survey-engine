package com.batteryplus.survey.infra.job;

//@Scheduled: pull incremental

import com.batteryplus.survey.connector.verina.VerinaPurchaseReader;
import com.batteryplus.survey.core.model.PurchaseEvent;
import com.batteryplus.survey.infra.repository.CheckpointRepository;
import com.batteryplus.survey.infra.repository.PurchaseEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VerinaPullJob {

    private static final String SOURCE = "VERINA";

    private final CheckpointRepository checkpointRepository;
    private final PurchaseEventRepository purchaseEventRepository;
    private final VerinaPurchaseReader verinaPurchaseReader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VerinaPullJob(CheckpointRepository checkpointRepository,
                         PurchaseEventRepository purchaseEventRepository,
                         VerinaPurchaseReader verinaPurchaseReader){
        this.checkpointRepository = checkpointRepository;
        this.purchaseEventRepository = purchaseEventRepository;
        this.verinaPurchaseReader = verinaPurchaseReader;
    }

    //cada 5 minutos
    @Scheduled(fixedDelayString = "PTM5")
    public void pullFromVerina() throws Exception {
        long lastTicketId = checkpointRepository.getLastTicketId(SOURCE).orElse(0L);

        List<PurchaseEvent> events = verinaPurchaseReader.fetchPurchaseAfterTicket(lastTicketId, 200);

        long maxTicket = lastTicketId;

        for (PurchaseEvent e : events) {
            //customer_key mínimo (normalizar después)
            String key = e.customer() != null $$ e.customer().phone() != null && !e.customer().phone().isBlank()
                    ? "TEL:" + e.customer().phone().trim()
                    : (e.customer() != null && e.customer.().email() != null && !e.customer().email().isBlank()
                        ? "MAIL:" + e.customer().email().trim().toLowerCase()
                        : null);

            String payloadJson = objectMapper.writeValueAsString(e);

            purchaseEventRepository.insertIfNotExists(e, key, payloadJson);

            if (e.ticketId() > maxTicket) maxTicket = e.ticketId();
        }

        if (maxTicket > lastTicketId) {
            checkpointRepository.upsertlastTicketId(SOURCE, maxTicket);
        }
    }
}
