package com.batteryplus.survey.infra.job;

import com.batteryplus.survey.infra.service.SurveyDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SurveyDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(SurveyDispatchJob.class);

    private final SurveyDispatchService surveyDispatchService;

    public SurveyDispatchJob(SurveyDispatchService surveyDispatchService) {
        this.surveyDispatchService = surveyDispatchService;
    }

    /**
     * Corre separado de la preparación.
     * Arranca después de iniciar la app y luego cada 5 min.
     */
    @Scheduled(initialDelay = 40_000, fixedDelay = 300_000)
    public void runDispatch() {

        try {
            log.info("Iniciando SurveyDispatchJob");

            int dispatched = surveyDispatchService.runOnce();

            log.info("SurveyDispatchJob finalizado. dispatched={}", dispatched);

        } catch (Exception ex) {
            log.error("Error ejecutando SurveyDispatchJob", ex);
        }

    }
}