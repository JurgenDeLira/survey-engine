package com.batteryplus.survey.api;

// reintentos/manual run (futuro)
import com.batteryplus.survey.adapter.clientify.ClientifyService;
import com.batteryplus.survey.core.model.VerinaTicketRow;
import com.batteryplus.survey.core.normalize.PhoneNormalizer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ClientifyService clientifyService;
    private final PhoneNormalizer phoneNormalizer;

    public AdminController(ClientifyService clientifyService, PhoneNormalizer phoneNormalizer) {
        this.clientifyService = clientifyService;
        this.phoneNormalizer = phoneNormalizer;
    }

    /**
     * Prueba rápida:
     * POST http://localhost:9080/admin/clientify/test
     * body:
     * {
     *   "phone": "6681507452",
     *   "ticket": "VERINA-7-12345",
     *   "nombre": "Jorge",
     *   "apellido": "De Lira",
     *   "email": "jorge@test.com"
     * }
     */
    @PostMapping("/clientify/test")
    public ResponseEntity<TestClientifyResponse> testClientify(@RequestBody TestClientifyRequest req) {

        String phoneE164 = phoneNormalizer.toE164OrNull(req.phone());
        if (phoneE164 == null) {
            return ResponseEntity.badRequest()
                    .body(new TestClientifyResponse(false, null, "Telefono inválido/no normalizable"));
        }

        if (req.ticket() == null || req.ticket().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new TestClientifyResponse(false, phoneE164, "ticket requerido"));
        }

        VerinaTicketRow fakeRow = new VerinaTicketRow(
                0,
                "",
                "",
                0L,
                LocalDateTime.now(),

                "",
                "",
                "",
                "",

                req.nombre() != null ? req.nombre() : "",
                req.apellido() != null ? req.apellido() : "",
                phoneE164,
                req.email(),

                "",
                "",
                null,
                null,

                "Mexico",
                "",
                "",
                "AdminTest",
                "venta"
        );

        boolean ok = clientifyService.upsertContactFromSale(
                phoneE164,
                req.ticket(),
                fakeRow
        );

        return ResponseEntity.ok(
                new TestClientifyResponse(
                        ok,
                        phoneE164,
                        ok ? "OK" : "No se pudo crear/actualizar contacto"
                )
        );
    }

    public record TestClientifyRequest(
            String phone,
            String ticket,
            String nombre,
            String apellido,
            String email
    ) {}

    public record TestClientifyResponse(boolean ok, String phoneE164, String message) {}
}