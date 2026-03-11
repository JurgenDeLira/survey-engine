package com.batteryplus.survey.infra.service;

import com.batteryplus.survey.infra.repository.SurveyDispatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SurveyDispatchService {

    private static final Logger log = LoggerFactory.getLogger(SurveyDispatchService.class);
    private static final int BATCH_SIZE = 50;

    private final SurveyDispatchRepository surveyDispatchRepository;
    private final SurveySenderService surveySenderService;

    public SurveyDispatchService(
            SurveyDispatchRepository surveyDispatchRepository,
            SurveySenderService surveySenderService
    ) {
        this.surveyDispatchRepository = surveyDispatchRepository;
        this.surveySenderService = surveySenderService;
    }

    public int runOnce() {
        var pending = surveyDispatchRepository.findPending(BATCH_SIZE);

        int sent = 0;

        log.info("Iniciando dispatch de encuestas. pending={}", pending.size());

        for (var row : pending) {
            try {
                boolean ok = surveySenderService.sendSurvey(
                        row.telefono(),
                        row.purchaseId(),
                        row.nombre()
                );

                if (ok) {
                    boolean marked = surveyDispatchRepository.markPrepared(row.purchaseId());
                    if (marked) {
                        sent++;
                    }
                }
            } catch (Exception ex) {
                log.error("Error enviando encuesta. purchaseId={} phone={}", row.purchaseId(), row.telefono(), ex);
            }
        }

        log.info("Dispatch de encuestas finalizado. sent={}", sent);
        return sent;
    }
}