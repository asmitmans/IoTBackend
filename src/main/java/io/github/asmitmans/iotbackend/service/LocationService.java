package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.location.LocationRequest;
import io.github.asmitmans.iotbackend.dto.location.LocationResponse;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.Location;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final AccountRepository accountRepository;

    public LocationService(LocationRepository locationRepository, AccountRepository accountRepository) {
        this.locationRepository = locationRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getAll(Long accountId) {
        return locationRepository.findByAccountId(accountId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LocationResponse getById(UUID publicId, Long accountId) {
        return toResponse(findOrThrow(publicId, accountId));
    }

    @Transactional
    public LocationResponse create(LocationRequest request, Long accountId) {
        Location parent = null;
        if (request.getParentPublicId() != null) {
            parent = findOrThrow(request.getParentPublicId(), accountId);
        }
        Long parentId = parent != null ? parent.getId() : null;

        if (locationRepository.existsByAccountIdAndParentIdAndName(accountId, parentId, request.getName())) {
            throw new ConflictException("Location with name '" + request.getName() +
                    "' already exists at this level");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Location entity = new Location();
        entity.setAccount(account);
        entity.setParent(parent);
        entity.setName(request.getName());

        return toResponse(locationRepository.save(entity));
    }

    @Transactional
    public LocationResponse update(UUID publicId, LocationRequest request, Long accountId) {
        Location entity = findOrThrow(publicId, accountId);

        Location parent = null;
        if (request.getParentPublicId() != null) {
            parent = findOrThrow(request.getParentPublicId(), accountId);
        }

        entity.setParent(parent);
        entity.setName(request.getName());

        return toResponse(locationRepository.save(entity));
    }

    @Transactional
    public void delete(UUID publicId, Long accountId) {
        Location entity = findOrThrow(publicId, accountId);
        locationRepository.delete(entity);
    }

    private Location findOrThrow(UUID publicId, Long accountId) {
        return locationRepository.findByPublicIdAndAccountId(publicId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
    }

    private LocationResponse toResponse(Location entity) {
        LocationResponse r = new LocationResponse();
        r.setPublicId(entity.getPublicId());
        r.setName(entity.getName());
        r.setParentPublicId(entity.getParent() != null ? entity.getParent().getPublicId() : null);
        return r;
    }

}