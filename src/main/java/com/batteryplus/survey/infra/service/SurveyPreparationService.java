package com.batteryplus.survey.infra.service;

import com.batteryplus.survey.core.normalize.PhoneNormalizer;
import com.batteryplus.survey.infra.repository.SurveyDispatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

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
                    log.warn(
                            "Venta pendiente no preparada por teléfono inválido. purchaseId={} tel={}",
                            row.purchaseId(), row.telefono()
                    );
                    continue;
                }

                boolean marked = surveyDispatchRepository.markPrepared(row.purchaseId());

                if (marked) {
                    prepared++;
                    log.info(
                            "Venta marcada lista para campaña. purchaseId={} propietario={} sucursal={} telefono={}",
                            row.purchaseId(), row.propietario(), row.sucursal(), phoneE164
                    );
                }
            } catch (Exception ex) {
                log.error(
                        "Error preparando venta para encuesta. purchaseId={} telefono={}",
                        row.purchaseId(), row.telefono(), ex
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