package com.batteryplus.survey.infra.job;

import com.batteryplus.survey.infra.service.SurveyDispatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SurveyDispatchJob {

    private final SurveyDispatchService surveyDispatchService;

    public SurveyDispatchJob(SurveyDispatchService surveyDispatchService) {
        this.surveyDispatchService = surveyDispatchService;
    }

    @Scheduled(fixedDelay = 300_000)
    public void dispatchSurveys() {
        surveyDispatchService.runOnce();
    }
}