package ma.transport.flotte.controller;

import ma.transport.flotte.dto.CamionCreateRequest;
import ma.transport.flotte.dto.CamionPatchRequest;
import ma.transport.flotte.dto.CamionResponse;
import ma.transport.flotte.dto.CamionsPageResponse;
import ma.transport.flotte.service.CamionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/camions")
public class CamionController {
    private final CamionService camionService;

    public CamionController(CamionService camionService) {
        this.camionService = camionService;
    }

    @GetMapping
    public CamionsPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Boolean disponible) {
        return camionService.list(page, size, disponible);
    }

    @PostMapping
    public ResponseEntity<CamionResponse> create(@Valid @RequestBody CamionCreateRequest request) {
        CamionResponse camion = camionService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/camions/" + camion.id())).body(camion);
    }

    @PatchMapping("/{id}")
    public CamionResponse update(
            @PathVariable @Size(min = 1, max = 64) String id,
            @RequestBody CamionPatchRequest request) {
        return camionService.update(id, request);
    }

    @GetMapping("/{id}")
    public CamionResponse getById(@PathVariable @Size(min = 1, max = 64) String id) {
        return camionService.getById(id);
    }
}
