package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {}
