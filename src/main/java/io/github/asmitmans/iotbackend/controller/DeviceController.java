package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.device.DeviceRegistrationRequest;
import io.github.asmitmans.iotbackend.dto.device.DeviceRegistrationResponse;
import io.github.asmitmans.iotbackend.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
}
