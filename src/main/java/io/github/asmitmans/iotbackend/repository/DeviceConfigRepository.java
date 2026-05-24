package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.DeviceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceConfigRepository extends JpaRepository<DeviceConfig,Long> {
}
