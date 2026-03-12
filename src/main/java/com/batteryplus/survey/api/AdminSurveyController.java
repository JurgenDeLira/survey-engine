package com.batteryplus.survey.api;

import com.batteryplus.survey.infra.service.SurveyDispatchService;
import com.batteryplus.survey.infra.service.SurveyPreparationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/surveys")
public class AdminSurveyController {

    private final SurveyPreparationService surveyPreparationService;
    private final SurveyDispatchService surveyDispatchService;

    public AdminSurveyController(
            SurveyPreparationService surveyPreparationService,
            SurveyDispatchService surveyDispatchService
    ) {
        this.surveyPreparationService = surveyPreparationService;
        this.surveyDispatchService = surveyDispatchService;
    }

    @PostMapping("/prepare")
    public ResponseEntity<RunResponse> prepare() {
        int processed = surveyPreparationService.runOnce();
        return ResponseEntity.ok(new RunResponse("prepare", processed, "OK"));
    }

    @PostMapping("/dispatch")
    public ResponseEntity<RunResponse> dispatch() {
        int processed = surveyDispatchService.runOnce();
        return ResponseEntity.ok(new RunResponse("dispatch", processed, "OK"));
    }

    @PostMapping("/run-all")
    public ResponseEntity<RunAllResponse> runAll() {
        int prepared = surveyPreparationService.runOnce();
        int dispatched = surveyDispatchService.runOnce();
        return ResponseEntity.ok(new RunAllResponse(prepared, dispatched, "OK"));
    }

    public record RunResponse(String action, int processed, String message) {}
    public record RunAllResponse(int prepared, int dispatched, String message) {}
}