package ma.transport.flotte.repository;

import ma.transport.flotte.model.Camion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CamionRepository extends JpaRepository<Camion, String> {
    @Query(value = """
            SELECT c.*
            FROM public.camion c
            WHERE (
                (:disponible = true
                    AND c.etat = 'EN_SERVICE'
                    AND c.archive = false
                    AND NOT EXISTS (
                        SELECT 1 FROM public.reservation_camion r
                        WHERE r.camion_id = c.id AND r.statut = 'ACTIVE'
                    ))
                OR
                (:disponible = false
                    AND (c.etat <> 'EN_SERVICE'
                        OR c.archive = true
                        OR EXISTS (
                            SELECT 1 FROM public.reservation_camion r
                            WHERE r.camion_id = c.id AND r.statut = 'ACTIVE'
                        )))
            )
            ORDER BY c.created_at DESC, c.id ASC
            """,
            countQuery = """
                    SELECT count(*)
                    FROM public.camion c
                    WHERE (
                        (:disponible = true
                            AND c.etat = 'EN_SERVICE'
                            AND c.archive = false
                            AND NOT EXISTS (
                                SELECT 1 FROM public.reservation_camion r
                                WHERE r.camion_id = c.id AND r.statut = 'ACTIVE'
                            ))
                        OR
                        (:disponible = false
                            AND (c.etat <> 'EN_SERVICE'
                                OR c.archive = true
                                OR EXISTS (
                                    SELECT 1 FROM public.reservation_camion r
                                    WHERE r.camion_id = c.id AND r.statut = 'ACTIVE'
                                )))
                    )
                    """,
            nativeQuery = true)
    Page<Camion> findByDisponibilite(@Param("disponible") boolean disponible, Pageable pageable);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM public.camion
                WHERE upper(regexp_replace(
                    regexp_replace(immatriculation, '^[[:space:]]+|[[:space:]]+$', '', 'g'),
                    '[[:space:]]+', ' ', 'g'
                )) = :immatriculation
            )
            """, nativeQuery = true)
    boolean existsByNormalizedImmatriculation(@Param("immatriculation") String immatriculation);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM public.camion
                WHERE id <> :id
                    AND upper(regexp_replace(
                        regexp_replace(immatriculation, '^[[:space:]]+|[[:space:]]+$', '', 'g'),
                        '[[:space:]]+', ' ', 'g'
                    )) = :immatriculation
            )
            """, nativeQuery = true)
    boolean existsByNormalizedImmatriculationAndIdNot(
            @Param("immatriculation") String immatriculation, @Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Camion c where c.id = :id")
    Optional<Camion> findByIdForUpdate(@Param("id") String id);
}
