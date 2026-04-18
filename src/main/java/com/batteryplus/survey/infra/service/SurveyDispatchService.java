package com.batteryplus.survey.infra.service;

import com.batteryplus.survey.adapter.clientify.ClientifyService;
import com.batteryplus.survey.infra.repository.SurveyDispatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.datasource.staging", name = "enabled", havingValue = "true")
public class SurveyDispatchService {

    private static final Logger log = LoggerFactory.getLogger(SurveyDispatchService.class);
    private static final int BATCH_SIZE = 100;

    private final SurveyDispatchRepository surveyDispatchRepository;
    private final SurveySenderService surveySenderService;
    private final ClientifyService clientifyService;

    public SurveyDispatchService(
            SurveyDispatchRepository surveyDispatchRepository,
            SurveySenderService surveySenderService,
            ClientifyService clientifyService
    ) {
        this.surveyDispatchRepository = surveyDispatchRepository;
        this.surveySenderService = surveySenderService;
        this.clientifyService = clientifyService;
    }

    public int runOnce() {
        var ready = surveyDispatchRepository.findReadyToDispatch(BATCH_SIZE);

        int dispatched = 0;
        int failed = 0;

        log.info("Iniciando dispatch de encuestas. ready={}", ready.size());

        for (var row : ready) {
            try {
                boolean ok = surveySenderService.sendSurvey(
                        row.telefono(),
                        row.purchaseId(),
                        row.nombre()
                );

                if (ok) {
                    boolean marked = surveyDispatchRepository.markDispatched(row.purchaseId());

                    if (marked) {
                        if (row.clientifyContactId() != null) {
                            boolean tagAdded = clientifyService.addSurveyTagToContact(row.clientifyContactId());

                            if (!tagAdded) {
                                log.warn(
                                        "No se pudo agregar tag de encuesta en Clientify. purchaseId={} telefono={} contactId={}",
                                        row.purchaseId(),
                                        row.telefono(),
                                        row.clientifyContactId()
                                );
                            }
                        } else {
                            log.warn(
                                    "No hay clientify_contact_id para agregar tag. purchaseId={} telefono={}",
                                    row.purchaseId(),
                                    row.telefono()
                            );
                        }

                        dispatched++;
                        log.info(
                                "Encuesta marcada como dispatched. purchaseId={} telefono={}",
                                row.purchaseId(),
                                row.telefono()
                        );
                    } else {
                        log.warn(
                                "No se pudo marcar dispatched en BD. purchaseId={}",
                                row.purchaseId()
                        );
                    }
                } else {
                    failed++;
                    surveyDispatchRepository.updateStatus(row.purchaseId(), "dispatch_error");

                    log.warn(
                            "El sender devolvió false. purchaseId={} telefono={}",
                            row.purchaseId(),
                            row.telefono()
                    );
                }

            } catch (Exception ex) {
                failed++;
                surveyDispatchRepository.updateStatus(row.purchaseId(), "dispatch_error");

                log.error(
                        "Error en dispatch de encuesta. purchaseId={} telefono={}",
                        row.purchaseId(),
                        row.telefono(),
                        ex
                );
            }
        }

        log.info(
                "Dispatch de encuestas finalizado. ready={} dispatched={} failed={}",
                ready.size(),
                dispatched,
                failed
        );

        return dispatched;
    }
}