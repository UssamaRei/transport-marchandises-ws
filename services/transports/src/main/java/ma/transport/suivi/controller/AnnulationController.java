package ma.transport.suivi.controller;

import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.service.AnnulationService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transports")
public class AnnulationController {

    private final AnnulationService service;

    public AnnulationController(AnnulationService service) {
        this.service = service;
    }

    @PostMapping("/{transportId}/annulation")
    public TransportResponse annuler(@PathVariable String transportId) {
        return service.annuler(transportId);
    }
}
