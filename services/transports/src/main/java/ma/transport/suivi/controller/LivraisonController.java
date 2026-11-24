package ma.transport.suivi.controller;

import ma.transport.suivi.dto.LivraisonRequest;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.service.LivraisonService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transports")
public class LivraisonController {

    private final LivraisonService service;

    public LivraisonController(LivraisonService service) {
        this.service = service;
    }

    @PostMapping("/{transportId}/livraison")
    public TransportResponse livrer(
            @PathVariable String transportId,
            @RequestBody LivraisonRequest demande
    ) {
        return service.livrer(transportId, demande);
    }
}
