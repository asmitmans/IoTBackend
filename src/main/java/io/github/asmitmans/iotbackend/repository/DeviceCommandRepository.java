package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface DeviceCommandRepository extends JpaRepository<DeviceCommand,
        Long> {

    Optional<DeviceCommand> findFirstByDeviceIdAndStatusOrderByQueuedAtAsc(Long deviceId, String status);

    boolean existsByDeviceIdAndStatus(Long deviceId, String status);

    List<DeviceCommand> findByDeviceIdAndStatusAndType(Long deviceId, String status, String type);
}
