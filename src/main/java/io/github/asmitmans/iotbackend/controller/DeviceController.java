package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.device.*;
import io.github.asmitmans.iotbackend.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceRegistrationResponse> register(
            @RequestBody @Valid DeviceRegistrationRequest request){
        DeviceRegistrationResponse response = deviceService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/enroll")
    public ResponseEntity<Void> enroll(
            @RequestBody @Valid DeviceEnrollRequest request,
            Authentication authentication) {
        deviceService.enroll(request.getSerialNumber(),
                             authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/claim")
    public ResponseEntity<DeviceClaimResponse> claim(
            @RequestBody @Valid DeviceClaimRequest request) {
        return ResponseEntity.ok(deviceService.claim(request.getSerialNumber()));
    }
}
