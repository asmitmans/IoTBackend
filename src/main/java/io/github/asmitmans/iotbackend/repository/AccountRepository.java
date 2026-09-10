package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByPublicId(UUID publicId);
    List<Account> findByJoinCodePrefix(String joinCodePrefix);

}