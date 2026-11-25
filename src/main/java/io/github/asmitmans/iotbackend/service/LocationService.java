package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.location.LocationRequest;
import io.github.asmitmans.iotbackend.dto.location.LocationResponse;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.Location;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import io.github.asmitmans.iotbackend.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    private final AccountRepository accountRepository;

    private final DeviceRepository deviceRepository;

    public LocationService(LocationRepository locationRepository, AccountRepository accountRepository,
            DeviceRepository deviceRepository) {
        this.locationRepository = locationRepository;
        this.accountRepository = accountRepository;
        this.deviceRepository = deviceRepository;
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
        Account account = lockAccount(accountId);

        Location parent = findParentOrNull(request, accountId);
        Long parentId = parent != null ? parent.getId() : null;

        if (locationRepository.existsByAccountIdAndParentIdAndName(accountId, parentId, request.getName())) {
            throw duplicateName(request.getName());
        }

        Location entity = new Location();
        entity.setAccount(account);
        entity.setParent(parent);
        entity.setName(request.getName());

        return toResponse(locationRepository.save(entity));
    }

    @Transactional
    public LocationResponse update(UUID publicId, LocationRequest request, Long accountId) {
        lockAccount(accountId);

        Location entity = findOrThrow(publicId, accountId);
        Location parent = findParentOrNull(request, accountId);
        Long parentId = parent != null ? parent.getId() : null;

        if (parent != null && familyOf(entity, accountId).contains(parent.getId())) {
            throw new ConflictException("Location cannot be moved under itself or its descendants");
        }

        if (locationRepository.existsByAccountIdAndParentIdAndNameAndIdNot(accountId, parentId, request.getName(),
                entity.getId())) {
            throw duplicateName(request.getName());
        }

        entity.setParent(parent);
        entity.setName(request.getName());

        return toResponse(locationRepository.save(entity));
    }

    @Transactional
    public void delete(UUID publicId, Long accountId) {
        lockAccount(accountId);

        Location entity = findOrThrow(publicId, accountId);

        if (locationRepository.existsByParentId(entity.getId())) {
            throw new ConflictException("Location has child locations; move or delete them first");
        }
        if (deviceRepository.existsByLocationId(entity.getId())) {
            throw new ConflictException("Location has devices assigned; unassign them first");
        }

        locationRepository.delete(entity);
    }

    private Account lockAccount(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private Location findParentOrNull(LocationRequest request, Long accountId) {
        if (request.getParentPublicId() == null) {
            return null;
        }
        return findOrThrow(request.getParentPublicId(), accountId);
    }

    private Set<Long> familyOf(Location root, Long accountId) {
        Map<Long, List<Long>> childIdsByParentId = locationRepository.findByAccountId(accountId)
            .stream()
            .filter(l -> l.getParent() != null)
            .collect(Collectors.groupingBy(l -> l.getParent().getId(),
                    Collectors.mapping(Location::getId, Collectors.toList())));

        Set<Long> family = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.push(root.getId());

        while (!pending.isEmpty()) {
            Long id = pending.pop();
            if (family.add(id)) {
                childIdsByParentId.getOrDefault(id, List.of()).forEach(pending::push);
            }
        }
        return family;
    }

    private ConflictException duplicateName(String name) {
        return new ConflictException("Location with name '" + name + "' already exists at this level");
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