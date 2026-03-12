package com.batteryplus.survey.infra.job;

import com.batteryplus.survey.infra.service.SurveyPreparationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SurveyPreparationJob {

    private final SurveyPreparationService surveyPreparationService;

    public SurveyPreparationJob(SurveyPreparationService surveyPreparationService) {
        this.surveyPreparationService = surveyPreparationService;
    }

    /**
     * Marca ventas como listas para encuesta.
     * Corre después del pull de Verina.
     */
    @Scheduled(initialDelay = 70_000, fixedDelay = 300_000)
    public void runPreparation() {
        surveyPreparationService.runOnce();
    }
}