package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByApiKeyHash(String apiKeyHash);
    Optional<Company> findByApiKeyPrefix(String apiKeyPrefix);

}
