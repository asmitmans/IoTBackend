package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

}
