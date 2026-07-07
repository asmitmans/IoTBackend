package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.measurement.MeasurementResponse;
import io.github.asmitmans.iotbackend.security.AccountResolver;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import io.github.asmitmans.iotbackend.service.MeasurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Tag(name = "Measurements", description = "Telemetry query endpoints")
@RestController
@RequestMapping("/api/v1/measurements")
public class MeasurementController {

    private final MeasurementService measurementService;
    private final AccountResolver accountResolver;

    public MeasurementController(MeasurementService measurementService, AccountResolver accountResolver) {
        this.measurementService = measurementService;
        this.accountResolver = accountResolver;
    }

    @Operation(summary = "Get latest measurement per device",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<MeasurementResponse>> getLatest(
            @RequestParam(required = false) List<Long> deviceIds,
            @RequestParam(required = false) Long accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long effectiveAccountId = accountResolver.resolveAccountId(principal, accountId);
        return ResponseEntity.ok(measurementService.getLatest(deviceIds, effectiveAccountId));
    }

    @Operation(summary = "Query measurements by time range",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<MeasurementResponse>> getByTimeRange(
            @RequestParam Long deviceId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long effectiveAccountId = accountResolver.resolveAccountId(principal, accountId);
        return ResponseEntity.ok(measurementService.getByTimeRange(deviceId, from, to, page, size, effectiveAccountId));
    }
}