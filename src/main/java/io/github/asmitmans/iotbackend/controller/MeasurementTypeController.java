package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.measurementtype.MeasurementTypeRequest;
import io.github.asmitmans.iotbackend.dto.measurementtype.MeasurementTypeResponse;
import io.github.asmitmans.iotbackend.service.MeasurementTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Measurement Types", description = "Global catalog of measurable variables")
@RestController
@RequestMapping("/api/v1/measurement-types")
public class MeasurementTypeController {

    private final MeasurementTypeService measurementTypeService;

    public MeasurementTypeController(MeasurementTypeService measurementTypeService) {
        this.measurementTypeService = measurementTypeService;
    }

    @Operation(summary = "List all measurement types", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<MeasurementTypeResponse>> getAll() {
        return ResponseEntity.ok(measurementTypeService.getAll());
    }

    @Operation(summary = "Get measurement type by ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<MeasurementTypeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(measurementTypeService.getById(id));
    }

    @Operation(summary = "Create measurement type", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeasurementTypeResponse> create(@RequestBody @Valid MeasurementTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(measurementTypeService.create(request));
    }

    @Operation(summary = "Update measurement type", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeasurementTypeResponse> update(@PathVariable Long id, @RequestBody @Valid MeasurementTypeRequest request) {
        return ResponseEntity.ok(measurementTypeService.update(id, request));
    }

    @Operation(summary = "Delete measurement type", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        measurementTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}