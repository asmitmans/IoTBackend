package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.ingest.IngestRequest;
import io.github.asmitmans.iotbackend.dto.ingest.IngestResponse;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.service.IngestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingest")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<IngestResponse> ingest(
            @RequestBody @Valid IngestRequest request,
            Authentication authentication) {

        Device device = (Device) authentication.getPrincipal();
        return ResponseEntity.ok(ingestService.ingest(request, device));
    }
}
