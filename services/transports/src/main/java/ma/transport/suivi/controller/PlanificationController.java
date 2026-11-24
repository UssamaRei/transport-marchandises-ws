package ma.transport.suivi.controller;

import ma.transport.suivi.dto.PlanificationRequest;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.service.PlanificationService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transports")
public class PlanificationController {

    private final PlanificationService service;

    public PlanificationController(PlanificationService service) {
        this.service = service;
    }

    @PostMapping
    public TransportResponse planifier(@RequestBody PlanificationRequest demande) {
        return service.planifier(demande);
    }
}
