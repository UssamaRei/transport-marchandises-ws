package ma.transport.suivi.repository;

import ma.transport.suivi.model.Transport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransportRepository
        extends JpaRepository<Transport, String>, JpaSpecificationExecutor<Transport> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transport t where t.id = :id")
    Optional<Transport> findByIdForUpdate(@Param("id") String id);

    Optional<Transport> findByOperationKey(String operationKey);
}