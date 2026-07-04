package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.location.LocationRequest;
import io.github.asmitmans.iotbackend.dto.location.LocationResponse;
import io.github.asmitmans.iotbackend.entity.Company;
import io.github.asmitmans.iotbackend.entity.Location;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.CompanyRepository;
import io.github.asmitmans.iotbackend.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final CompanyRepository companyRepository;

    public LocationService(LocationRepository locationRepository, CompanyRepository companyRepository) {
        this.locationRepository = locationRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getAll(Long companyId) {
        return locationRepository.findByCompanyId(companyId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LocationResponse getById(Long id, Long companyId) {
        return toResponse(findOrThrow(id, companyId));
    }

    @Transactional
    public LocationResponse create(LocationRequest request, Long companyId) {
        Long parentId = request.getParentId();

        if (locationRepository.existsByCompanyIdAndParentIdAndName(companyId, parentId, request.getName())) {
            throw new ConflictException("Location with name '" + request.getName() +
                    "' already exists at this level");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Location parent = null;
        if (parentId != null) {
            parent = findOrThrow(parentId, companyId);
        }

        Location entity = new Location();
        entity.setCompany(company);
        entity.setParent(parent);
        entity.setName(request.getName());

        return toResponse(locationRepository.save(entity));
    }

    @Transactional
    public LocationResponse update(Long id, LocationRequest request, Long companyId) {
        Location entity = findOrThrow(id, companyId);

        Location parent = null;
        if (request.getParentId() != null) {
            parent = findOrThrow(request.getParentId(), companyId);
        }

        entity.setParent(parent);
        entity.setName(request.getName());

        return toResponse(locationRepository.save(entity));
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        Location entity = findOrThrow(id, companyId);
        locationRepository.delete(entity);
    }

    private Location findOrThrow(Long id, Long companyId) {
        return locationRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
    }

    private LocationResponse toResponse(Location entity) {
        LocationResponse r = new LocationResponse();
        r.setId(entity.getId());
        r.setName(entity.getName());
        r.setParentId(entity.getParent() != null ? entity.getParent().getId() : null);
        return r;
    }

}
