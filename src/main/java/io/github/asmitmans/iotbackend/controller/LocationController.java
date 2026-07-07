package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.location.LocationRequest;
import io.github.asmitmans.iotbackend.dto.location.LocationResponse;
import io.github.asmitmans.iotbackend.security.AccountResolver;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import io.github.asmitmans.iotbackend.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Locations", description = "Hierarchical location management per account")
@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;
    private final AccountResolver accountResolver;

    public LocationController(LocationService locationService, AccountResolver accountResolver) {
        this.locationService = locationService;
        this.accountResolver = accountResolver;
    }

    @Operation(summary = "List locations", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LocationResponse>> getAll(
            @RequestParam(required = false) Long accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(locationService.getAll(accountResolver.resolveAccountId(principal, accountId)));
    }

    @Operation(summary = "Get location by ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationResponse> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(locationService.getById(id, accountResolver.resolveAccountId(principal, accountId)));
    }

    @Operation(summary = "Create location", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationResponse> create(
            @RequestBody @Valid LocationRequest request,
            @RequestParam(required = false) Long accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(locationService.create(request, accountResolver.resolveAccountId(principal, accountId)));
    }

    @Operation(summary = "Update location", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid LocationRequest request,
            @RequestParam(required = false) Long accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(locationService.update(id, request, accountResolver.resolveAccountId(principal, accountId)));
    }

    @Operation(summary = "Delete location", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(required = false) Long accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        locationService.delete(id, accountResolver.resolveAccountId(principal, accountId));
        return ResponseEntity.noContent().build();
    }
}