package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.account.*;
import io.github.asmitmans.iotbackend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Accounts", description = "Account creation and self-service management")
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Create a new account", description = "Authenticated user must not already belong to an account. Creator becomes OWNER.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<AccountRegistrationResponse> create(
            @RequestBody @Valid AccountRegistrationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(accountService.create(authentication.getName(), request));
    }

    @Operation(summary = "List my accounts", description = "Returns every account the authenticated user has a membership in, with their role in each.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/mine")
    public ResponseEntity<List<AccountMembershipResponse>> getMine(Authentication authentication) {
        return ResponseEntity.ok(accountService.getMine(authentication.getName()));
    }

    @Operation(summary = "Switch active account", description = "Issues a new JWT scoped to the given account. Requires an existing membership — does not require re-entering the password.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{accountId}/switch")
    public ResponseEntity<AccountRegistrationResponse> switchAccount(
            @PathVariable UUID accountId,
            Authentication authentication) {
        return ResponseEntity.ok(accountService.switchAccount(authentication.getName(), accountId));
    }

    @Operation(summary = "Generate/rotate join code", description = "OWNER only. Returns the plaintext code once — not retrievable again. Valid 24h.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{accountId}/join-code")
    public ResponseEntity<JoinCodeResponse> generateJoinCode(
            @PathVariable UUID accountId,
            Authentication authentication) {
        return ResponseEntity.ok(accountService.generateJoinCode(authentication.getName(), accountId));
    }

    @Operation(summary = "Join an account using its join code", description = "No accountId needed — the code alone identifies the account. Joiner is added as MEMBER.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/join")
    public ResponseEntity<AccountRegistrationResponse> join(
            @RequestBody @Valid AccountJoinRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(accountService.join(authentication.getName(), request.getJoinCode()));
    }

    @Operation(summary = "Leave an account", description = "Self-removal. Blocked if you are the only OWNER — promote another member first.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{accountId}/leave")
    public ResponseEntity<Void> leave(
            @PathVariable UUID accountId,
            Authentication authentication) {
        accountService.leave(authentication.getName(), accountId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove a member from the account", description = "OWNER only. Cannot remove the only OWNER (prevents orphaned accounts).", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{accountId}/members/{username}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID accountId,
            @PathVariable String username,
            Authentication authentication) {
        accountService.removeMember(authentication.getName(), accountId, username);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Promote a member to OWNER", description = "OWNER only.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{accountId}/members/{username}/promote")
    public ResponseEntity<Void> promote(
            @PathVariable UUID accountId,
            @PathVariable String username,
            Authentication authentication) {
        accountService.promote(authentication.getName(), accountId, username);
        return ResponseEntity.noContent().build();
    }
}