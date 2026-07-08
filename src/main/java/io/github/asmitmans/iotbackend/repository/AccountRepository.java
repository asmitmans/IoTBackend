package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByApiKeyHash(String apiKeyHash);
    Optional<Account> findByApiKeyPrefix(String apiKeyPrefix);
    Optional<Account> findByPublicId(UUID publicId);

}