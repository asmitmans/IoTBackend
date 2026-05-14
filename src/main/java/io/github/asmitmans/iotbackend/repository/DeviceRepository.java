package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByApiKeyPrefix(String apiKeyPrefix);
    Optional<Device> findBySerialNumber(String serialNumber);
}
