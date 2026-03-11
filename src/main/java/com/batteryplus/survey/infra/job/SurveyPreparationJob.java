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

    @Scheduled(fixedDelay = 300_000)
    public void prepareSurveys() {
        surveyPreparationService.runOnce();
    }
}