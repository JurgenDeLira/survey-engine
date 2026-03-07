package com.batteryplus.survey.infra.job;

//@Scheduled: pull incremental

import com.batteryplus.survey.infra.service.VerinaPullService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.jobs.verinaPull.enabled", havingValue = "true")
public class VerinaPullJob {

    private final VerinaPullService service;

    public VerinaPullJob(VerinaPullService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 300_000)
    public void pullFromVerina() throws Exception {
        service.runOnce();
    }
}