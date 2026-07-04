package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.DeviceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceModelRepository extends JpaRepository<DeviceModel, Long> {
    Optional<DeviceModel> findByName(String name);
}
