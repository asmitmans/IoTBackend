package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.CommandType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommandTypeRepository extends JpaRepository<CommandType,Long> {
    Optional<CommandType> findByName(String name);
}
