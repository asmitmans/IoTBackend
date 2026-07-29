package io.github.asmitmans.iotbackend.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "account_membership")
public class AccountMembership extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_role", nullable = false, length = 20)
    private AccountRole accountRole;

    public AccountMembership() {
    }

    public AccountMembership(User user, Account account, AccountRole accountRole) {
        this.user = user;
        this.account = account;
        this.accountRole = accountRole;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public AccountRole getAccountRole() {
        return accountRole;
    }

    public void setAccountRole(AccountRole accountRole) {
        this.accountRole = accountRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AccountMembership that = (AccountMembership) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}