package ma.transport.suivi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.repository.TransportRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransportService {

    private final TransportRepository repository;

    public TransportService(TransportRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public TransportResponse getById(String id) {
        Transport transport = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transport introuvable : " + id
                ));

        return TransportResponse.from(transport);
    }

    @Transactional(readOnly = true)
    public Page<TransportResponse> list(
            String camionId,
            boolean actifs,
            StatutTransport statut,
            String recherche,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La page doit être positive ou nulle et size doit être entre 1 et 100"
            );
        }

        Specification<Transport> filtres = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();

            if (camionId != null && !camionId.isBlank()) {
                conditions.add(cb.equal(root.get("camionId"), camionId));
            }

            if (actifs) {
                conditions.add(
                        root.get("statut")
                                .in(StatutTransport.LIVRE, StatutTransport.ANNULE)
                                .not()
                );
            }

            if (statut != null) {
                conditions.add(cb.equal(root.get("statut"), statut));
            }

            if (recherche != null && !recherche.isBlank()) {
                String terme = "%" + recherche.toLowerCase(Locale.ROOT) + "%";

                conditions.add(cb.or(
                        cb.like(cb.lower(root.get("id")), terme),
                        cb.like(cb.lower(root.get("destination")), terme)
                ));
            }

            return cb.and(conditions.toArray(new Predicate[0]));
        };

        PageRequest pagination = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        return repository.findAll(filtres, pagination)
                .map(TransportResponse::from);
    }
}