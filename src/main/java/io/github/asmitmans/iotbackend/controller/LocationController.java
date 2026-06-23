package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.location.LocationRequest;
import io.github.asmitmans.iotbackend.dto.location.LocationResponse;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import io.github.asmitmans.iotbackend.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LocationResponse>> getAll(@RequestParam(required = false) Long companyId,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(locationService.getAll(resolveCompanyId(principal, companyId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationResponse> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(locationService.getById(id,resolveCompanyId(principal, companyId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationResponse> create(
            @RequestBody @Valid LocationRequest request,
            @RequestParam(required = false) Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(locationService
                                           .create(request, resolveCompanyId(principal, companyId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid LocationRequest request,
            @RequestParam(required = false) Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(locationService.update(id, request,
                                                        resolveCompanyId(principal, companyId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(required = false) Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        locationService.delete(id, resolveCompanyId(principal, companyId));
        return ResponseEntity.noContent().build();
    }

    private Long resolveCompanyId(UserPrincipal principal, Long requestedCompanyId) {
        boolean isAdmin = principal.getAuthorities().stream()
                                   .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            if (requestedCompanyId == null) {
                throw new IllegalArgumentException("Admin must provide companyId");
            }
            return requestedCompanyId;
        }

        if (principal.getCompanyId() == null) {
            throw new AccessDeniedException("User has no associated company");
        }
        return principal.getCompanyId();
    }
}
