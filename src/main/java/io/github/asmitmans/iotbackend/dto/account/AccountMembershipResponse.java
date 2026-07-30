package io.github.asmitmans.iotbackend.dto.account;

import io.github.asmitmans.iotbackend.entity.AccountRole;

import java.util.UUID;

public class AccountMembershipResponse {

    private UUID publicId;
    private String name;
    private AccountRole accountRole;

    public AccountMembershipResponse() {
    }

    public AccountMembershipResponse(UUID publicId, String name, AccountRole accountRole) {
        this.publicId = publicId;
        this.name = name;
        this.accountRole = accountRole;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountRole getAccountRole() {
        return accountRole;
    }

    public void setAccountRole(AccountRole accountRole) {
        this.accountRole = accountRole;
    }
}
