package ma.transport.chargements.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import ma.transport.chargements.dto.ChargementRequest;
import ma.transport.chargements.dto.ChargementResume;
import ma.transport.chargements.dto.LigneChargementRequest;
import ma.transport.chargements.dto.ValidationChargementRequest;
import ma.transport.chargements.model.Chargement;
import ma.transport.chargements.service.ChargementService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chargements")
@Validated
public class ChargementController {

    private final ChargementService chargementService;

    public ChargementController(ChargementService chargementService) {
        this.chargementService = chargementService;
    }

    @PostMapping
    public ResponseEntity<Chargement> create(@Valid @RequestBody ChargementRequest request) {
        Chargement chargement = chargementService.create(request);
        URI location = URI.create("/api/v1/chargements/" + chargement.getId());
        return ResponseEntity.created(location).body(chargement);
    }

    @GetMapping
    public List<ChargementResume> findAll(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return chargementService.findAll(limit, offset);
    }

    @GetMapping("/{id}")
    public Chargement findById(@PathVariable String id) {
        return chargementService.findById(id);
    }

    @PutMapping("/{id}")
    public Chargement update(@PathVariable String id, @Valid @RequestBody ChargementRequest request) {
        return chargementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        chargementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lignes")
    public ResponseEntity<Chargement> addLigne(@PathVariable String id,
                                             @Valid @RequestBody LigneChargementRequest request) {
        Chargement chargement = chargementService.addLigne(id, request);
        return ResponseEntity.status(201).body(chargement);
    }

    @PutMapping("/{id}/lignes/{ligneId}")
    public Chargement updateLigne(@PathVariable String id, @PathVariable String ligneId,
                                  @Valid @RequestBody LigneChargementRequest request) {
        return chargementService.updateLigne(id, ligneId, request);
    }

    @DeleteMapping("/{id}/lignes/{ligneId}")
    public Chargement deleteLigne(@PathVariable String id, @PathVariable String ligneId) {
        return chargementService.deleteLigne(id, ligneId);
    }

    @PostMapping("/{id}/validation")
    public Chargement validate(@PathVariable String id,
                               @Valid @RequestBody ValidationChargementRequest request) {
        return chargementService.validate(id, request.getCamionId().trim());
    }
}
