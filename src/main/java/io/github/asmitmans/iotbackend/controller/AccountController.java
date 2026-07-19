package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.account.AccountRegistrationRequest;
import io.github.asmitmans.iotbackend.dto.account.AccountRegistrationResponse;
import io.github.asmitmans.iotbackend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}