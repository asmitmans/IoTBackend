package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByCompanyId(Long companyId);

    Optional<Location> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndParentIdAndName(Long companyId, Long parentId, String name);

}
