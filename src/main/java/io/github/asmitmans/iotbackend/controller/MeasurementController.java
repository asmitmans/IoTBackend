package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.measurement.MeasurementResponse;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import io.github.asmitmans.iotbackend.service.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/measurements")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<MeasurementResponse>> getLatest(
            @RequestParam(required = false) List<Long> deviceIds,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                measurementService.getLatest(deviceIds, principal.getCompanyId()));
    }
}