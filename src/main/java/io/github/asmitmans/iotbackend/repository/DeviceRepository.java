package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Device;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByApiKeyPrefix(String apiKeyPrefix);
    Optional<Device> findBySerialNumber(String serialNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Device d WHERE d.serialNumber = :serial")
    Optional<Device> findBySerialNumberForUpdate(@Param("serial") String serial);
}
