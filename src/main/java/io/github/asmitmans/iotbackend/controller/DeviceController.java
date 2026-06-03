package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.device.*;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import io.github.asmitmans.iotbackend.service.DeviceConfigService;
import io.github.asmitmans.iotbackend.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceConfigService deviceConfigService;

    public DeviceController(DeviceService deviceService,
                            DeviceConfigService deviceConfigService) {
        this.deviceService = deviceService;
        this.deviceConfigService = deviceConfigService;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setConfig(
            @PathVariable Long deviceId,
            @RequestBody @Valid AdminConfigRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
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
}