package io.github.asmitmans.iotbackend.entity;

import io.github.asmitmans.iotbackend.util.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.util.UUID;

/**
 * Base for entities exposed externally via an opaque UUID (public_id)
 * while keeping an internal BIGINT PK for joins/FKs. See Account, Device.
 */
@MappedSuperclass
public abstract class PubliclyIdentifiableEntity extends AuditableEntity {

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @PrePersist
    protected void generatePublicId() {
        if (this.publicId == null) {
            this.publicId = UuidV7Generator.generate();
        }
    }

    public UUID getPublicId() {
        return publicId;
    }
}