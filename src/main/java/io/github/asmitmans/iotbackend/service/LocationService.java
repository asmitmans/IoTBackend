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
    public LocationResponse getById(Long id, Long accountId) {
        return toResponse(findOrThrow(id, accountId));
    }

    @Transactional
    public LocationResponse create(LocationRequest request, Long accountId) {
        Long parentId = request.getParentId();

        if (locationRepository.existsByAccountIdAndParentIdAndName(accountId, parentId, request.getName())) {
            throw new ConflictException("Location with name '" + request.getName() +
                                                "' already exists at this level");
        }

        Account account = accountRepository.findById(accountId)
                                           .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Location parent = null;
        if (parentId != null) {
            parent = findOrThrow(parentId, accountId);
        }

        Location entity = new Location();
        entity.setAccount(account);
        entity.setParent(parent);
        entity.setName(request.getName());

        return toResponse(locationRepository.save(entity));
    }

    @Transactional
    public LocationResponse update(Long id, LocationRequest request, Long accountId) {
        Location entity = findOrThrow(id, accountId);

        Location parent = null;
        if (request.getParentId() != null) {
            parent = findOrThrow(request.getParentId(), accountId);
        }

        entity.setParent(parent);
        entity.setName(request.getName());

        return toResponse(locationRepository.save(entity));
    }

    @Transactional
    public void delete(Long id, Long accountId) {
        Location entity = findOrThrow(id, accountId);
        locationRepository.delete(entity);
    }

    private Location findOrThrow(Long id, Long accountId) {
        return locationRepository.findByIdAndAccountId(id, accountId)
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