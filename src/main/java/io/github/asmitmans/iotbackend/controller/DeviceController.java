package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.device.*;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.security.AccountResolver;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import io.github.asmitmans.iotbackend.service.DeviceCommandService;
import io.github.asmitmans.iotbackend.service.DeviceConfigService;
import io.github.asmitmans.iotbackend.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Devices", description = "Device lifecycle, config, state and command management")
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceConfigService deviceConfigService;
    private final DeviceCommandService deviceCommandService;
    private final CacheManager cacheManager;
    private final AccountResolver accountResolver;

    public DeviceController(DeviceService deviceService,
                            DeviceConfigService deviceConfigService,
                            DeviceCommandService deviceCommandService,
                            CacheManager cacheManager,
                            AccountResolver accountResolver
    ) {
        this.deviceService = deviceService;
        this.deviceConfigService = deviceConfigService;
        this.deviceCommandService = deviceCommandService;
        this.cacheManager = cacheManager;
        this.accountResolver = accountResolver;
    }

    @Operation(summary = "List devices", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<DeviceListResponse>> getAll(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long effectiveAccountId = accountResolver.resolveAccountId(principal, accountId);
        return ResponseEntity.ok(deviceService.getAll(effectiveAccountId, page, size));
    }

    @Operation(summary = "Get device detail", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{devicePublicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DeviceDetailResponse> getById(
            @PathVariable UUID devicePublicId,
            @RequestParam(required = false) UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long effectiveAccountId = accountResolver.resolveAccountId(principal, accountId);
        Long deviceId = deviceService.resolveDeviceId(devicePublicId);
        return ResponseEntity.ok(deviceService.getById(deviceId, effectiveAccountId));
    }

    @Operation(summary = "Register device (platform admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceRegistrationResponse> register(
            @RequestBody @Valid DeviceRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.register(request));
    }

    @Operation(summary = "Enroll device to account", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/enroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DeviceEnrollResponse> enroll(
            @RequestBody @Valid DeviceEnrollRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getAccountId() == null) {
            throw new AccessDeniedException("User has no active account to enroll into");
        }
        return ResponseEntity.ok(deviceService.enroll(request.getSerialNumber(), principal.getAccountId(), request.getName()));
    }

    @Operation(summary = "Claim device and obtain API key", description = "Public endpoint — no authentication required")
    @PostMapping("/claim")
    public ResponseEntity<DeviceClaimResponse> claim(
            @RequestBody @Valid DeviceClaimRequest request) {
        return ResponseEntity.ok(deviceService.claim(request.getSerialNumber()));
    }

    @Operation(summary = "Get device desired config", security = @SecurityRequirement(name = "ApiKey"))
    @GetMapping("/{devicePublicId}/config")
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<DeviceConfigResponse> getConfig(
            @PathVariable UUID devicePublicId,
            Authentication authentication) {
        Device device = (Device) authentication.getPrincipal();
        return ResponseEntity.ok(deviceConfigService.getConfig(device));
    }

    @Operation(summary = "Set device desired config", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{devicePublicId}/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> setConfig(
            @PathVariable UUID devicePublicId,
            @RequestBody @Valid AdminConfigRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                                   .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long accountId = accountResolver.resolveOwnAccountIdOrNull(principal);
        if (!isAdmin && accountId == null) {
            throw new AccessDeniedException("User has no associated account");
        }
        Long deviceId = deviceService.resolveDeviceId(devicePublicId);
        deviceConfigService.setDesiredConfig(deviceId, request, accountId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Report device state", security = @SecurityRequirement(name = "ApiKey"))
    @PostMapping("/{devicePublicId}/state")
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<Void> reportState(
            @PathVariable UUID devicePublicId,
            @RequestBody @Valid DeviceStateRequest request,
            Authentication authentication) {
        Device device = (Device) authentication.getPrincipal();
        deviceConfigService.reportState(device, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Create command for device", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{devicePublicId}/commands")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> createCommand(
            @PathVariable UUID devicePublicId,
            @RequestBody @Valid CreateCommandRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                                   .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long accountId = accountResolver.resolveOwnAccountIdOrNull(principal);
        if (!isAdmin && accountId == null) {
            throw new AccessDeniedException("User has no associated account");
        }
        Long deviceId = deviceService.resolveDeviceId(devicePublicId);
        deviceCommandService.createCommand(deviceId, request, accountId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Get next pending command", security = @SecurityRequirement(name = "ApiKey"))
    @GetMapping("/{devicePublicId}/commands/next")
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<DeviceCommandResponse> getNextCommand(
            @PathVariable UUID devicePublicId,
            Authentication authentication) {
        Device device = (Device) authentication.getPrincipal();
        return deviceCommandService.getNextCommand(device)
                                   .map(ResponseEntity::ok)
                                   .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Update device status", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{devicePublicId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID devicePublicId,
            @RequestParam String status) {
        Long deviceId = deviceService.resolveDeviceId(devicePublicId);
        deviceService.updateStatus(deviceId, status);
        cacheManager.getCache("deviceAuth").clear();
        return ResponseEntity.ok().build();
    }
}