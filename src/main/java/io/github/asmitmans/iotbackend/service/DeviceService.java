package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.device.*;
import io.github.asmitmans.iotbackend.entity.*;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final LocationRepository locationRepository;
    private final ApiKeyService apiKeyService;

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public DeviceService(DeviceRepository deviceRepository, UserRepository userRepository, DeviceModelRepository deviceModelRepository, LocationRepository locationRepository, ApiKeyService apiKeyService) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.deviceModelRepository = deviceModelRepository;
        this.locationRepository = locationRepository;
        this.apiKeyService = apiKeyService;
    }

    /**
     * Translates the externally-facing publicId into the internal id used
     * by services/repositories. Mirrors AccountResolver's role, kept here
     * since no admin/role branching is involved for device lookup.
     */
    @Transactional(readOnly = true)
    public Long resolveDeviceId(UUID publicId) {
        return deviceRepository.findByPublicId(publicId)
                               .map(Device::getId)
                               .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    }

    public DeviceRegistrationResponse register(DeviceRegistrationRequest request) {

        String serialNumber;
        do {
            serialNumber = generateSerial();
        } while (deviceRepository.findBySerialNumber(serialNumber).isPresent());

        DeviceModel model = deviceModelRepository.findById(request.getDeviceModelId())
                                                 .orElseThrow(() -> new ResourceNotFoundException("Device model not found"));

        Location location = null;
        if (request.getLocationId() != null) {
            location = locationRepository.findById(request.getLocationId())
                                         .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
        }

        Device device = new Device();
        device.setSerialNumber(serialNumber);
        device.setName(request.getName());
        device.setDeviceModel(model);
        device.setLocation(location);
        device.setStatus("UNCLAIMED");

        device = deviceRepository.save(device);

        DeviceRegistrationResponse response = new DeviceRegistrationResponse();
        response.setPublicId(device.getPublicId());
        response.setSerialNumber(device.getSerialNumber());
        response.setName(device.getName());
        response.setStatus(device.getStatus());

        return response;
    }

    @Transactional
    public void enroll(String serialNumber, String username) {
        Device device = deviceRepository.findBySerialNumberForUpdate(serialNumber)
                                        .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (!device.getStatus().equals("UNCLAIMED")) {
            throw new ConflictException("Device is not available for " +
                                                "enrollment");
        }

        User user = userRepository.findByUsername(username)
                                  .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = user.getAccount();
        if (account == null) {
            throw new ConflictException("User has no associated account");
        }

        device.setAccount(account);
        device.setStatus("PENDING");
        device.setClaimExpiresAt(Instant.now().plusSeconds(300));

        deviceRepository.save(device);
    }

    @Transactional
    public DeviceClaimResponse claim(String serialNumber) {
        Device device =
                deviceRepository
                        .findBySerialNumber(serialNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("Device not " + "found"));

        if (!device.getStatus().equals("PENDING")) {
            throw new ConflictException("Device is not pending enrollment");
        }

        if (Instant.now().isAfter(device.getClaimExpiresAt())) {
            device.setStatus("UNCLAIMED");
            device.setAccount(null);
            device.setClaimExpiresAt(null);
            deviceRepository.save(device);
            throw new ConflictException("Enrollment window expired");
        }

        String apiKeyPlain = apiKeyService.generate("iotdev_");

        device.setApiKeyHash(apiKeyService.hash(apiKeyPlain));
        device.setApiKeyPrefix(apiKeyService.extractPrefix(apiKeyPlain));
        device.setClaimedAt(Instant.now());
        device.setClaimExpiresAt(null);
        device.setStatus("ACTIVE");

        deviceRepository.save(device);

        DeviceClaimResponse response = new DeviceClaimResponse();
        response.setSerialNumber(device.getSerialNumber());
        response.setStatus(device.getStatus());
        response.setApiKeyPlain(apiKeyPlain);

        return response;
    }

    @Transactional
    public void updateStatus(Long deviceId, String newStatus) {
        Device device = deviceRepository.findById(deviceId)
                                        .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        List<String> validStatuses = List.of("ACTIVE", "INACTIVE");
        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus);
        }

        device.setStatus(newStatus);
        deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public Page<DeviceListResponse> getAll(Long accountId, int page, int size) {
        int safeSize = Math.min(size, 500);
        Pageable pageable = PageRequest.of(page, safeSize);

        return deviceRepository.findByAccountId(accountId, pageable)
                               .map(this::toListResponse);
    }

    @Transactional(readOnly = true)
    public DeviceDetailResponse getById(Long deviceId, Long accountId) {
        Device device = deviceRepository.findByIdAndAccountId(deviceId, accountId)
                                        .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
        return toDetailResponse(device);
    }


    private DeviceListResponse toListResponse(Device d) {
        DeviceListResponse r = new DeviceListResponse();
        r.setPublicId(d.getPublicId());
        r.setSerialNumber(d.getSerialNumber());
        r.setName(d.getName());
        r.setStatus(d.getStatus());
        r.setDeviceModelId(d.getDeviceModel().getId());
        r.setDeviceModelName(d.getDeviceModel().getName());
        r.setLocationId(d.getLocation() != null ? d.getLocation().getId() : null);
        return r;
    }

    private DeviceDetailResponse toDetailResponse(Device d) {
        DeviceDetailResponse r = new DeviceDetailResponse();
        r.setPublicId(d.getPublicId());
        r.setSerialNumber(d.getSerialNumber());
        r.setName(d.getName());
        r.setStatus(d.getStatus());
        r.setDeviceModelId(d.getDeviceModel().getId());
        r.setDeviceModelName(d.getDeviceModel().getName());
        r.setLocationId(d.getLocation() != null ? d.getLocation().getId() : null);
        r.setConfigPending(d.isConfigPending());
        r.setCommandPending(d.isCommandPending());
        return r;
    }
    private String generateSerial() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

}