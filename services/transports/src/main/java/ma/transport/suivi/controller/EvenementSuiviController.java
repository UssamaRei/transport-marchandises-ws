package ma.transport.suivi.controller;

import ma.transport.suivi.dto.EvenementRequest;
import ma.transport.suivi.dto.EvenementResponse;
import ma.transport.suivi.service.EvenementSuiviService;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transports/{transportId}/evenements")
public class EvenementSuiviController {

    private final EvenementSuiviService service;

    public EvenementSuiviController(EvenementSuiviService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvenementResponse ajouter(
            @PathVariable String transportId,
            @RequestBody EvenementRequest request
    ) {
        return service.ajouter(transportId, request);
    }

    @GetMapping
    public Page<EvenementResponse> lister(
            @PathVariable String transportId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.lister(transportId, page, size);
    }
}
