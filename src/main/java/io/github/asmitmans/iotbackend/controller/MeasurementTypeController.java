package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.measurementtype.MeasurementTypeRequest;
import io.github.asmitmans.iotbackend.dto.measurementtype.MeasurementTypeResponse;
import io.github.asmitmans.iotbackend.service.MeasurementTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/measurement-types")
public class MeasurementTypeController {

    private final MeasurementTypeService measurementTypeService;

    public MeasurementTypeController(MeasurementTypeService measurementTypeService) {
        this.measurementTypeService = measurementTypeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<MeasurementTypeResponse>> getAll() {
        return ResponseEntity.ok(measurementTypeService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<MeasurementTypeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(measurementTypeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeasurementTypeResponse> create(@RequestBody @Valid MeasurementTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(measurementTypeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeasurementTypeResponse> update(@PathVariable Long id, @RequestBody @Valid MeasurementTypeRequest request) {
        return ResponseEntity.ok(measurementTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        measurementTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}































