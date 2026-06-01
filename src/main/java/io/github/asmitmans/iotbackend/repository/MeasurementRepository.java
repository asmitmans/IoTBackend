package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Measurement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {
    @Query("SELECT m FROM Measurement m WHERE m.device.id = :deviceId " +
            "ORDER BY m.recordedAt DESC LIMIT 1")

    Optional<Measurement> findLatestByDeviceId(@Param("deviceId") Long deviceId);

    @Query("SELECT m FROM Measurement m WHERE m.device.id = :deviceId " +
            "AND m.companyId = :companyId " +
            "AND m.recordedAt BETWEEN :from AND :to " +
            "ORDER BY m.recordedAt DESC")
    Page<Measurement> findByDeviceIdAndTimeRange(
            @Param("deviceId") Long deviceId,
            @Param("companyId") Long companyId,
            @Param("from")Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
