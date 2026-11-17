package ma.transport.flotte.controller;

import ma.transport.flotte.dto.ReservationCreateRequest;
import ma.transport.flotte.dto.ReservationReleaseRequest;
import ma.transport.flotte.dto.ReservationResponse;
import ma.transport.flotte.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request) {
        ReservationResponse reservation = reservationService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/reservations/" + reservation.id()))
                .body(reservation);
    }

    @GetMapping("/{id}")
    public ReservationResponse getById(@PathVariable @NotBlank @Size(max = 64) String id) {
        return reservationService.getById(id);
    }

    @GetMapping
    public ReservationResponse getByTransportId(
            @RequestParam @NotBlank @Size(max = 64) String transportId) {
        return reservationService.getByTransportId(transportId);
    }

    @PostMapping("/{id}/liberation")
    public ReservationResponse release(
            @PathVariable @NotBlank @Size(max = 64) String id,
            @Valid @RequestBody ReservationReleaseRequest request) {
        return reservationService.release(id, request);
    }
}
