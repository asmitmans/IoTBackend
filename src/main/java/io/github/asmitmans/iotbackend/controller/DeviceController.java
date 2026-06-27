package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.device.*;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.security.CompanyResolver;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import io.github.asmitmans.iotbackend.service.DeviceCommandService;
import io.github.asmitmans.iotbackend.service.DeviceConfigService;
import io.github.asmitmans.iotbackend.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceConfigService deviceConfigService;
    private final DeviceCommandService deviceCommandService;
    private final CacheManager cacheManager;
    private final CompanyResolver companyResolver;

    public DeviceController(DeviceService deviceService,
                            DeviceConfigService deviceConfigService, DeviceCommandService deviceCommandService, CacheManager cacheManager, CompanyResolver companyResolver
    ) {
        this.deviceService = deviceService;
        this.deviceConfigService = deviceConfigService;
        this.deviceCommandService = deviceCommandService;
        this.cacheManager = cacheManager;
        this.companyResolver = companyResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<DeviceListResponse>> getAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long effectiveCompanyId = companyResolver.resolveCompanyId(principal, companyId);
        return ResponseEntity.ok(deviceService.getAll(effectiveCompanyId, page, size));
    }

    @GetMapping("/{deviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DeviceDetailResponse> getById(
            @PathVariable Long deviceId,
            @RequestParam(required = false) Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long effectiveCompanyId = companyResolver.resolveCompanyId(principal, companyId);
        return ResponseEntity.ok(deviceService.getById(deviceId, effectiveCompanyId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceRegistrationResponse> register(
            @RequestBody @Valid DeviceRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(deviceService.register(request));
    }

    @PostMapping("/enroll")
    public ResponseEntity<Void> enroll(
            @RequestBody @Valid DeviceEnrollRequest request,
            Authentication authentication) {
        deviceService.enroll(request.getSerialNumber(), authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/claim")
    public ResponseEntity<DeviceClaimResponse> claim(
            @RequestBody @Valid DeviceClaimRequest request) {
        return ResponseEntity.ok(deviceService.claim(request.getSerialNumber()));
    }

    @GetMapping("/{deviceId}/config")
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<DeviceConfigResponse> getConfig(
            @PathVariable Long deviceId,
            Authentication authentication) {
        Device device = (Device) authentication.getPrincipal();
        return ResponseEntity.ok(deviceConfigService.getConfig(device));
    }

    @PutMapping("/{deviceId}/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> setConfig(
            @PathVariable Long deviceId,
            @RequestBody @Valid AdminConfigRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!principal.getAuthorities().stream()
                      .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                && principal.getCompanyId() == null) {
            throw new AccessDeniedException("User has no associated company");
        }

        deviceConfigService.setDesiredConfig(deviceId, request, principal.getCompanyId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/state")
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<Void> reportState(
            @PathVariable Long deviceId,
            @RequestBody @Valid DeviceStateRequest request,
            Authentication authentication) {
        Device device = (Device) authentication.getPrincipal();
        deviceConfigService.reportState(device, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/commands")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> createCommand(
            @PathVariable Long deviceId,
            @RequestBody @Valid CreateCommandRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!principal.getAuthorities().stream()
                      .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                && principal.getCompanyId() == null) {
            throw new AccessDeniedException("User has no associated company");
        }

        deviceCommandService.createCommand(deviceId, request, principal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{deviceId}/commands/next")
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<DeviceCommandResponse> getNextCommand(
            @PathVariable Long deviceId,
            Authentication authentication) {
        Device device = (Device) authentication.getPrincipal();
        return deviceCommandService.getNextCommand(device)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PatchMapping("/{deviceId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long deviceId,
            @RequestParam String status) {
        deviceService.updateStatus(deviceId, status);
        cacheManager.getCache("deviceAuth").clear();
        return ResponseEntity.ok().build();
    }
}