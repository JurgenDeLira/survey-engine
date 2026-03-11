package com.batteryplus.survey.infra.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SurveySenderService {

    private static final Logger log = LoggerFactory.getLogger(SurveySenderService.class);

    public boolean sendSurvey(String phone, String purchaseId, String nombre) {
        log.info("Simulando envío de encuesta. phone={} purchaseId={} nombre={}", phone, purchaseId, nombre);

        // Aquí después voy a conectar:
        // - WhatsApp API
        // - Clientify automation trigger
        // - webhook
        // - lo que tenga que definir

        return true;
    }
}