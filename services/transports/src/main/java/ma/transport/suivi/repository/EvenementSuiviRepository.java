package ma.transport.suivi.repository;

import ma.transport.suivi.model.EvenementSuivi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvenementSuiviRepository
        extends JpaRepository<EvenementSuivi, String> {

    Page<EvenementSuivi> findByTransportIdOrderByDateEvenementAscIdAsc(
            String transportId,
            Pageable pageable
    );
}
