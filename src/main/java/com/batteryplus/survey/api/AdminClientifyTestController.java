package com.batteryplus.survey.api;

import com.batteryplus.survey.adapter.clientify.ClientifyService;
import com.batteryplus.survey.core.model.VerinaTicketRow;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/admin/clientify")
public class AdminClientifyTestController {

    private final ClientifyService clientifyService;

    public AdminClientifyTestController(ClientifyService clientifyService) {
        this.clientifyService = clientifyService;
    }

    @PostMapping("/test-upsert")
    public ResponseEntity<?> testUpsert(@RequestBody TestClientifyRequest request) {
        try {
            VerinaTicketRow row = new VerinaTicketRow(
                    request.idSucursal(),
                    request.sucursal(),
                    request.me14Sucursal(),
                    request.ticket(),
                    parseDateTime(request.fechaUltimaCompra()),

                    request.propietario(),
                    request.meGama(),
                    request.meMarcaBateria(),
                    request.meBateriaAdquirida(),

                    request.nombre(),
                    request.apellido(),
                    request.telefono(),
                    request.correoElectronico(),

                    request.meMarcaAuto(),
                    request.meModeloAuto(),
                    request.meAnioAuto(),
                    request.meFechaFinGarantia(),

                    request.pais(),
                    request.estadoProvincia(),
                    request.ciudad(),
                    request.origen(),
                    request.estado()
            );

            boolean ok = clientifyService.upsertContactFromSale(
                    request.phoneE164(),
                    request.ticketValue(),
                    row
            );

            return ResponseEntity.ok(new TestClientifyResponse(
                    ok,
                    request.phoneE164(),
                    request.ticketValue(),
                    "Prueba ejecutada. Revisa logs de ClientifyService."
            ));

        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(
                    "Error ejecutando prueba Clientify",
                    ex.getMessage()
            ));
        }
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "fechaUltimaCompra debe venir en formato ISO, por ejemplo 2026-03-14T11:58:20"
            );
        }
    }

    public record TestClientifyRequest(
            String phoneE164,
            String ticketValue,

            int idSucursal,
            String sucursal,
            String me14Sucursal,
            long ticket,
            String fechaUltimaCompra,

            String propietario,
            String meGama,
            String meMarcaBateria,
            String meBateriaAdquirida,

            String nombre,
            String apellido,
            String telefono,
            String correoElectronico,

            String meMarcaAuto,
            String meModeloAuto,
            Integer meAnioAuto,
            String meFechaFinGarantia,

            String pais,
            String estadoProvincia,
            String ciudad,
            String origen,
            String estado
    ) {}

    public record TestClientifyResponse(
            boolean ok,
            String phoneE164,
            String ticketValue,
            String message
    ) {}

    public record ErrorResponse(
            String error,
            String detail
    ) {}
}