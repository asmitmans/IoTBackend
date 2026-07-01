package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.ingest.IngestRequest;
import io.github.asmitmans.iotbackend.dto.ingest.IngestResponse;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.service.IngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ingest", description = "Device telemetry ingestion")
@RestController
@RequestMapping("/api/v1/ingest")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @Operation(summary = "Submit telemetry data",
            description = "Authenticated by device API key. Returns pending flags for config and commands.",
            security = @SecurityRequirement(name = "ApiKey"))
    @PostMapping
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<IngestResponse> ingest(
            @RequestBody @Valid IngestRequest request,
            Authentication authentication) {

        Device device = (Device) authentication.getPrincipal();
        return ResponseEntity.ok(ingestService.ingest(request, device));
    }
}