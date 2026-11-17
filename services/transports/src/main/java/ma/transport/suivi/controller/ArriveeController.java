package ma.transport.suivi.controller;

import ma.transport.suivi.dto.ArriveeRequest;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.service.ArriveeService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transports")
public class ArriveeController {

    private final ArriveeService service;

    public ArriveeController(ArriveeService service) {
        this.service = service;
    }

    @PostMapping("/{transportId}/arrivee")
    public TransportResponse enregistrer(
            @PathVariable("transportId") String transportId,
            @RequestBody ArriveeRequest demande
    ) {
        return service.enregistrer(transportId, demande.arriveeReelle());
    }
}