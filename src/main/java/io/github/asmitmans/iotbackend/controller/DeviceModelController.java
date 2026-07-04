package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.devicemodel.DeviceModelDetailResponse;
import io.github.asmitmans.iotbackend.dto.devicemodel.DeviceModelListResponse;
import io.github.asmitmans.iotbackend.dto.devicemodel.DeviceModelRequest;
import io.github.asmitmans.iotbackend.service.DeviceModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Device Models", description = "Device model management")
@RestController
@RequestMapping("/api/v1/device-models")
public class DeviceModelController {

    private final DeviceModelService deviceModelService;

    public DeviceModelController(DeviceModelService deviceModelService) {
        this.deviceModelService = deviceModelService;
    }

    @Operation(summary = "List all device models", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DeviceModelListResponse>> getAll() {
        return ResponseEntity.ok(deviceModelService.getAll());
    }

    @Operation(summary = "Get device model detail", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DeviceModelDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(deviceModelService.getById(id));
    }

    @Operation(summary = "Create device model", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceModelDetailResponse> create(@RequestBody @Valid DeviceModelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceModelService.create(request));
    }

    @Operation(summary = "Update device model", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceModelDetailResponse> update(@PathVariable Long id, @RequestBody @Valid DeviceModelRequest request) {
        return ResponseEntity.ok(deviceModelService.update(id, request));
    }

    @Operation(summary = "Delete device model", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deviceModelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}