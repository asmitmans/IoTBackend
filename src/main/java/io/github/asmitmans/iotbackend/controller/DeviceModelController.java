package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.devicemodel.DeviceModelDetailResponse;
import io.github.asmitmans.iotbackend.dto.devicemodel.DeviceModelListResponse;
import io.github.asmitmans.iotbackend.dto.devicemodel.DeviceModelRequest;
import io.github.asmitmans.iotbackend.service.DeviceModelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/device-models")
public class DeviceModelController {

    private final DeviceModelService deviceModelService;

    public DeviceModelController(DeviceModelService deviceModelService) {
        this.deviceModelService = deviceModelService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DeviceModelListResponse>> getAll() {
        return ResponseEntity.ok(deviceModelService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DeviceModelDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(deviceModelService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceModelDetailResponse> create(@RequestBody @Valid DeviceModelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceModelService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceModelDetailResponse> update(@PathVariable Long id, @RequestBody @Valid DeviceModelRequest request) {
        return ResponseEntity.ok(deviceModelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deviceModelService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
