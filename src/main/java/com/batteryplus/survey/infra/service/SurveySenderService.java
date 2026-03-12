package com.batteryplus.survey.infra.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SurveySenderService {

    private static final Logger log = LoggerFactory.getLogger(SurveySenderService.class);

    /**
     * Stub temporal.
     * Aquí después conectarás el disparo real hacia Clientify/campaña.
     */
    public boolean sendSurvey(String telefono, String purchaseId, String nombre) {
        log.info(
                "SIMULACION envio encuesta. purchaseId={} telefono={} nombre={}",
                purchaseId, telefono, nombre
        );

        return true;
    }
}