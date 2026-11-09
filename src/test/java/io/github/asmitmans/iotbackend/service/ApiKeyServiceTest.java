package io.github.asmitmans.iotbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiKeyServiceTest {

    private static final String DEVICE_PREFIX = "iotdev_";

    private ApiKeyService service;

    @BeforeEach
    void setUp() {
        // Low BCrypt cost keeps the test fast; the cost factor does not change hash/verify semantics.
        service = new ApiKeyService(new BCryptPasswordEncoder(4));
    }

    // ---------- generate() ----------

    @Test
    void generate_returnsThePrefixFollowedBy64HexChars() {
        String key = service.generate(DEVICE_PREFIX);

        assertThat(key).matches("iotdev_[0-9a-f]{64}");
    }

    @Test
    void generate_returnsADifferentKeyEachTime() {
        assertThat(service.generate(DEVICE_PREFIX)).isNotEqualTo(service.generate(DEVICE_PREFIX));
    }

    @Test
    void generate_producesDistinctLookupPrefixes() {
        Set<String> prefixes = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            prefixes.add(service.extractPrefix(service.generate(DEVICE_PREFIX)));
        }

        assertThat(prefixes).hasSize(200);
    }

    // ---------- extractPrefix() ----------

    @Test
    void extractPrefix_returnsTheFirst16Chars() {
        String key = service.generate(DEVICE_PREFIX);

        assertThat(service.extractPrefix(key))
                .hasSize(16)
                .isEqualTo(key.substring(0, 16));
    }

    @Test
    void extractPrefix_withKeyShorterThanThePrefixLength_returnsTheWholeKey() {
        assertThat(service.extractPrefix("iotdev_")).isEqualTo("iotdev_");
    }

    @Test
    void extractPrefix_withKeyOfExactlyThePrefixLength_returnsTheWholeKey() {
        assertThat(service.extractPrefix("iotdev_123456789")).isEqualTo("iotdev_123456789");
    }

    // ---------- hash() / verify() ----------

    @Test
    void hash_doesNotStoreThePlainKey() {
        String key = service.generate(DEVICE_PREFIX);

        assertThat(service.hash(key)).isNotEqualTo(key).doesNotContain(key);
    }

    @Test
    void verify_withTheOriginalKey_returnsTrue() {
        String key = service.generate(DEVICE_PREFIX);

        assertThat(service.verify(key, service.hash(key))).isTrue();
    }

    @Test
    void verify_withADifferentKey_returnsFalse() {
        String hash = service.hash(service.generate(DEVICE_PREFIX));

        assertThat(service.verify(service.generate(DEVICE_PREFIX), hash)).isFalse();
    }
}