package io.github.asmitmans.iotbackend.repository;

import io.github.asmitmans.iotbackend.entity.AccountMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountMembershipRepository extends JpaRepository<AccountMembership, Long> {

    List<AccountMembership> findByUserId(Integer userId);

    Optional<AccountMembership> findByUserIdAndAccountId(Integer userId, Long accountId);

    boolean existsByUserIdAndAccountId(Integer userId, Long accountId);
}