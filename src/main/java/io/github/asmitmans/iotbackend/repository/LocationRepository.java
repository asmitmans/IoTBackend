package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByAccountId(Long accountId);

    Optional<Location> findByPublicIdAndAccountId(UUID publicId, Long accountId);

    boolean existsByAccountIdAndParentIdAndName(Long accountId, Long parentId, String name);

}