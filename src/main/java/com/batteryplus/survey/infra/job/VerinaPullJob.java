package com.batteryplus.survey.infra.job;

import com.batteryplus.survey.infra.service.VerinaPullService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VerinaPullJob {

    private static final Logger log = LoggerFactory.getLogger(VerinaPullJob.class);

    private final VerinaPullService verinaPullService;

    public VerinaPullJob(VerinaPullService verinaPullService) {
        this.verinaPullService = verinaPullService;
    }

    /**
     * Ejecuta pull de ventas desde Verina.
     * Arranca primero al iniciar la app.
     */
    @Scheduled(initialDelay = 10_000, fixedDelay = 300_000)
    public void runPull() {
        try {
            verinaPullService.runOnce();
        } catch (Exception ex) {
            log.error("Error ejecutando VerinaPullJob", ex);
        }
    }
}