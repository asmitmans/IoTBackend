package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.location.LocationRequest;
import io.github.asmitmans.iotbackend.dto.location.LocationResponse;
import io.github.asmitmans.iotbackend.security.CompanyResolver;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import io.github.asmitmans.iotbackend.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;
    private final CompanyResolver companyResolver;

    public LocationController(LocationService locationService, CompanyResolver companyResolver) {
        this.locationService = locationService;
        this.companyResolver = companyResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LocationResponse>> getAll(
            @RequestParam(required = false) Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                locationService.getAll(companyResolver.resolveCompanyId(principal, companyId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationResponse> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                locationService.getById(id, companyResolver.resolveCompanyId(principal, companyId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationResponse> create(
            @RequestBody @Valid LocationRequest request,
            @RequestParam(required = false) Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(locationService.create(request, companyResolver.resolveCompanyId(principal, companyId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid LocationRequest request,
            @RequestParam(required = false) Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                locationService.update(id, request, companyResolver.resolveCompanyId(principal, companyId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(required = false) Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        locationService.delete(id, companyResolver.resolveCompanyId(principal, companyId));
        return ResponseEntity.noContent().build();
    }
}