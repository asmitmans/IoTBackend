package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.CommandType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CommandTypeRepository extends JpaRepository<CommandType,Long> {
    Optional<CommandType> findByName(String name);
    List<CommandType> findByIdIn(Set<Long> ids);
}
