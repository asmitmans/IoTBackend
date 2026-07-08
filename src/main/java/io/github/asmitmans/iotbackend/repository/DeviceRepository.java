package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Device;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByApiKeyPrefix(String apiKeyPrefix);
    Optional<Device> findBySerialNumber(String serialNumber);
    Optional<Device> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Device d WHERE d.serialNumber = :serial")
    Optional<Device> findBySerialNumberForUpdate(@Param("serial") String serial);

    List<Device> findByAccountId(Long accountId);
    List<Device> findByIdInAndAccountId(List<Long> ids, Long accountId);

    Page<Device> findByAccountId(Long accountId, Pageable pageable);

    Optional<Device> findByIdAndAccountId(Long id, Long accountId);
}