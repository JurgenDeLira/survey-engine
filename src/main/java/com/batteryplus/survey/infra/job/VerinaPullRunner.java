package com.batteryplus.survey.infra.job;

import com.batteryplus.survey.infra.service.VerinaPullService;
import com.batteryplus.survey.core.model.VerinaSaleRow;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@ConditionalOnProperty(name = "app.jobs.verinaPull.runner.enabled", havingValue = "true")
@Component
public class VerinaPullRunner implements CommandLineRunner {

    private final VerinaPullService service;

    public VerinaPullRunner(VerinaPullService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        System.out.println("== VerinaPullRunner: iniciando prueba de lectura ==");

        // En esta prueba: NO actualices checkpoint hasta que veas que todo sale bien.
        boolean updateCheckpoint = false;

        List<VerinaSaleRow> rows = service.pullNewSales(updateCheckpoint);

        System.out.println("Ventas nuevas encontradas: " + rows.size());
        rows.stream().limit(10).forEach(r ->
                System.out.printf("Ticket=%s suc=%d fecha=%s tel=%s email=%s%n",
                        r.ticket(), r.idSucursal(), r.fecha(), r.telefono(), r.email())
        );

        System.out.println("== VerinaPullRunner: fin ==");
    }
}