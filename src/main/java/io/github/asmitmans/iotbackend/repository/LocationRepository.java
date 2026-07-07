package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByAccountId(Long accountId);

    Optional<Location> findByIdAndAccountId(Long id, Long accountId);

    boolean existsByAccountIdAndParentIdAndName(Long accountId, Long parentId, String name);

}