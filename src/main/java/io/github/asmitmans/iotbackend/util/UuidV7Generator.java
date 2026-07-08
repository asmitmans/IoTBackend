package io.github.asmitmans.iotbackend.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generates RFC 9562 UUIDv7 values: 48-bit Unix timestamp (ms) followed by
 * random bits, giving monotonically increasing, B-tree-friendly identifiers
 * unlike random UUIDv4.
 */
public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7Generator() {
    }

    public static UUID generate() {
        long timestampMs = System.currentTimeMillis();
        byte[] uuidBytes = new byte[16];

        // bytes 0-5: 48-bit big-endian timestamp
        uuidBytes[0] = (byte) (timestampMs >>> 40);
        uuidBytes[1] = (byte) (timestampMs >>> 32);
        uuidBytes[2] = (byte) (timestampMs >>> 24);
        uuidBytes[3] = (byte) (timestampMs >>> 16);
        uuidBytes[4] = (byte) (timestampMs >>> 8);
        uuidBytes[5] = (byte) timestampMs;

        // bytes 6-15: random
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);
        System.arraycopy(random, 0, uuidBytes, 6, 10);

        // byte 6 high nibble: version 0111 (7)
        uuidBytes[6] = (byte) (0x70 | (uuidBytes[6] & 0x0F));

        // byte 8 high 2 bits: variant 10
        uuidBytes[8] = (byte) (0x80 | (uuidBytes[8] & 0x3F));

        long msb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (uuidBytes[i] & 0xFF);
        }
        long lsb = 0;
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (uuidBytes[i] & 0xFF);
        }

        return new UUID(msb, lsb);
    }
}