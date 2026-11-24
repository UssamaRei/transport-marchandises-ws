package ma.transport.suivi.controller;

import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.service.TransportService;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transports")
public class TransportController {

    private final TransportService service;

    public TransportController(TransportService service) {
        this.service = service;
    }

    @GetMapping
    public Page<TransportResponse> list(
            @RequestParam(required = false) String camionId,
            @RequestParam(defaultValue = "false") boolean actifs,
            @RequestParam(required = false) StatutTransport statut,
            @RequestParam(required = false) String recherche,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(camionId, actifs, statut, recherche, page, size);
    }

    @GetMapping("/{id}")
    public TransportResponse getById(@PathVariable String id) {
        return service.getById(id);
    }
}
