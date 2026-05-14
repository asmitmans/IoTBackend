package io.github.asmitmans.iotbackend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class ApiKeyService {

    private static final int KEY_BYTES = 32;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String generate() {
        byte[] bytes = new byte[KEY_BYTES];
        secureRandom.nextBytes(bytes);
        return "iotdev_" + HexFormat.of().formatHex(bytes);
    }

    public String extracPrefix(String apiKey) {
        return apiKey.substring(0, 8);
    }

    public String hash(String apiKey) {
        return passwordEncoder.encode(apiKey);
    }

    public boolean verify(String apiKey, String hash) {
        return passwordEncoder.matches(apiKey, hash);
    }
}
