package io.github.asmitmans.iotbackend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "account")
public class Account extends PubliclyIdentifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_name", nullable = false, length = 100)
    private String name;

    @Column(name = "join_code_hash", length = 255)
    private String joinCodeHash;

    @Column(name = "join_code_prefix", length = 10)
    private String joinCodePrefix;

    @Column(name = "join_code_expires_at")
    private Instant joinCodeExpiresAt;

    public Account() {
    }

    public Account(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJoinCodeHash() {
        return joinCodeHash;
    }

    public void setJoinCodeHash(String joinCodeHash) {
        this.joinCodeHash = joinCodeHash;
    }

    public String getJoinCodePrefix() {
        return joinCodePrefix;
    }

    public void setJoinCodePrefix(String joinCodePrefix) {
        this.joinCodePrefix = joinCodePrefix;
    }

    public Instant getJoinCodeExpiresAt() {
        return joinCodeExpiresAt;
    }

    public void setJoinCodeExpiresAt(Instant joinCodeExpiresAt) {
        this.joinCodeExpiresAt = joinCodeExpiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}