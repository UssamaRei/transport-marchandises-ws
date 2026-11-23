package ma.transport.bons.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import ma.transport.bons.model.BonLivraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BonLivraisonRepository extends JpaRepository<BonLivraison, String> {
    org.springframework.data.domain.Page<BonLivraison> findByStatut(
            ma.transport.bons.model.StatutBon statut, org.springframework.data.domain.Pageable pageable);

    Optional<BonLivraison> findByChargementId(String chargementId);

    @EntityGraph(attributePaths = "lignes")
    @Query("select b from BonLivraison b where b.id = :id")
    Optional<BonLivraison> findDetailById(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BonLivraison b where b.id = :id")
    Optional<BonLivraison> findByIdForUpdate(@Param("id") String id);
}
