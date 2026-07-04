package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.DeviceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceConfigRepository extends JpaRepository<DeviceConfig,Long> {

    List<DeviceConfig> findByDeviceId(Long deviceId);

    Optional<DeviceConfig> findByDeviceIdAndKey(Long deviceId, String key);

    void deleteByDeviceId(Long deviceId);
}
