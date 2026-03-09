package com.batteryplus.survey.infra.job;

import com.batteryplus.survey.infra.service.VerinaPullService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.jobs.verinaPull.runner.enabled", havingValue = "true")
public class VerinaPullRunner implements CommandLineRunner {

    private final VerinaPullService service;

    public VerinaPullRunner(VerinaPullService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("== VerinaPullRunner: ejecutando ingestión manual ==");
        int processed = service.runOnce();
        System.out.println("== VerinaPullRunner: fin. processed=" + processed + " ==");
    }
}