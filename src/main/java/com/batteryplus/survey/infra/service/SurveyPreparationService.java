package com.batteryplus.survey.infra.service;

import com.batteryplus.survey.core.normalize.PhoneNormalizer;
import com.batteryplus.survey.infra.repository.SurveyDispatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@ConditionalOnProperty(prefix = "app.datasource.staging", name = "enabled", havingValue = "true")
@Service
public class SurveyPreparationService {

    private static final Logger log = LoggerFactory.getLogger(SurveyPreparationService.class);
    private static final int BATCH_SIZE = 100;

    private final SurveyDispatchRepository surveyDispatchRepository;
    private final PhoneNormalizer phoneNormalizer;

    public SurveyPreparationService(
            SurveyDispatchRepository surveyDispatchRepository,
            PhoneNormalizer phoneNormalizer
    ) {
        this.surveyDispatchRepository = surveyDispatchRepository;
        this.phoneNormalizer = phoneNormalizer;
    }

    public int runOnce() {
        List<SurveyDispatchRepository.PendingSurveyRow> pending =
                surveyDispatchRepository.findPending(BATCH_SIZE);

        int prepared = 0;
        int invalidPhone = 0;

        log.info("Iniciando preparación de encuestas. pending={}", pending.size());

        for (SurveyDispatchRepository.PendingSurveyRow row : pending) {
            try {
                String phoneE164 = phoneNormalizer.toE164OrNull(row.telefono());

                if (phoneE164 == null) {
                    invalidPhone++;

                    surveyDispatchRepository.updateStatus(row.purchaseId(), "invalid_phone");

                    log.warn(
                            "Venta pendiente no preparada por teléfono inválido. purchaseId={} tel={}",
                            row.purchaseId(), row.telefono()
                    );
                    continue;
                }

                boolean marked = surveyDispatchRepository.updateStatus(
                        row.purchaseId(),
                        "ready_for_campaign"
                );

                if (marked) {
                    prepared++;
                    log.info(
                            "Venta marcada lista para campaña. purchaseId={} propietario={} sucursal={} telefono={}",
                            row.purchaseId(), row.propietario(), row.sucursal(), phoneE164
                    );
                } else {
                    log.warn(
                            "No se pudo marcar ready_for_campaign. purchaseId={}",
                            row.purchaseId()
                    );
                }

            } catch (Exception ex) {
                log.error(
                        "Error preparando encuesta. purchaseId={}",
                        row.purchaseId(),
                        ex
                );
            }
        }

        log.info(
                "Preparación de encuestas finalizada. pending={} prepared={} invalidPhone={}",
                pending.size(), prepared, invalidPhone
        );

        return prepared;
    }
}