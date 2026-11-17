package ma.transport.flotte.repository;

import ma.transport.flotte.model.ReservationCamion;
import ma.transport.flotte.model.StatutReservation;
import java.math.BigDecimal;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationCamionRepository extends JpaRepository<ReservationCamion, String> {
    boolean existsByCamionIdAndStatut(String camionId, StatutReservation statut);

    Optional<ReservationCamion> findByTransportId(String transportId);

    @Query("select r.camionId from ReservationCamion r where r.id = :id")
    Optional<String> findCamionIdById(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservationCamion r where r.id = :id")
    Optional<ReservationCamion> findByIdForUpdate(@Param("id") String id);

    @Query("""
            select sum(r.poidsKg)
            from ReservationCamion r
            where r.camionId = :camionId and r.statut = :statut
            """)
    BigDecimal sumPoidsByCamionIdAndStatut(
            @Param("camionId") String camionId, @Param("statut") StatutReservation statut);
}
