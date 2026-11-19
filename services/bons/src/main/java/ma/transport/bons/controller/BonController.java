package ma.transport.bons.controller;

import jakarta.validation.Valid;
import java.net.URI;
import ma.transport.bons.dto.BonResponse;
import ma.transport.bons.dto.CreerBonRequest;
import ma.transport.bons.dto.PreuveLivraisonRequest;
import ma.transport.bons.dto.PreuveLivraisonResponse;
import ma.transport.bons.service.BonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bons")
public class BonController {
    private final BonService service;
    public BonController(BonService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<BonResponse> creer(@Valid @RequestBody CreerBonRequest request) {
        BonResponse bon = BonResponse.from(service.creer(request));
        return ResponseEntity.created(URI.create("/api/v1/bons/" + bon.id())).body(bon);
    }

    @GetMapping("/{id}")
    public BonResponse obtenir(@PathVariable String id) {
        return BonResponse.from(service.obtenir(id));
    }

    @PostMapping("/{id}/preuve-livraison")
    public PreuveLivraisonResponse confirmer(@PathVariable String id,
                                               @Valid @RequestBody PreuveLivraisonRequest request) {
        return service.confirmer(id, request);
    }
}
