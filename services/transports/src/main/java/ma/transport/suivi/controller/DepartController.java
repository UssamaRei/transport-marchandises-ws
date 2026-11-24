package ma.transport.suivi.controller;

import ma.transport.suivi.dto.DepartRequest;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.service.DepartService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transports")
public class DepartController {

    private final DepartService service;

    public DepartController(DepartService service) {
        this.service = service;
    }

    @PostMapping("/{transportId}/depart")
    public TransportResponse declarerDepart(
            @PathVariable String transportId,
            @RequestBody DepartRequest request
    ) {
        return service.declarerDepart(transportId, request);
    }
}
