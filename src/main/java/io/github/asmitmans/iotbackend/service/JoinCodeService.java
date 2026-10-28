package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.security.JoinCodeProperties;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Service
public class JoinCodeService {

    private final JoinCodeProperties props;
    private final ApiKeyService apiKeyService;
    private final AccountRepository accountRepository;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public JoinCodeService(JoinCodeProperties props,
                           ApiKeyService apiKeyService,
                           AccountRepository accountRepository,
                           Clock clock) {
        this.props = props;
        this.apiKeyService = apiKeyService;
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    public JoinCodeIssuance issue() {
        String plainCode = generateCode();
        String hash = apiKeyService.hash(plainCode);
        String prefix = plainCode.substring(0, props.prefixLength());
        Instant expiresAt = Instant.now(clock).plus(props.validityHours(), ChronoUnit.HOURS);


        return new JoinCodeIssuance(plainCode, hash, prefix, expiresAt);
    }

    public Account resolveAndVerify(String rawCode) {
        String prefix = rawCode.length() >= props.prefixLength()
                ? rawCode.substring(0, props.prefixLength())
                : rawCode;

        Account account = accountRepository.findByJoinCodePrefix(prefix).stream()
                                           .filter(a -> a.getJoinCodeHash() != null)
                                           .filter(a -> apiKeyService.verify(rawCode, a.getJoinCodeHash()))
                                           .findFirst()
                                           .orElseThrow(() -> new AccessDeniedException("Invalid join code"));

        if (Instant.now(clock).isAfter(account.getJoinCodeExpiresAt())) {
            throw new ConflictException("Join code has expired");
        }

        return account;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < props.length(); i++) {
            sb.append(props.alphabet().charAt(secureRandom.nextInt(props.alphabet().length())));
        }
        return sb.toString();
    }
}