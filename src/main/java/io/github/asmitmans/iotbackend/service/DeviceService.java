package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.device.DeviceClaimResponse;
import io.github.asmitmans.iotbackend.dto.device.DeviceRegistrationRequest;
import io.github.asmitmans.iotbackend.dto.device.DeviceRegistrationResponse;
import io.github.asmitmans.iotbackend.entity.Company;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.DeviceModel;
import io.github.asmitmans.iotbackend.entity.Location;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.CompanyRepository;
import io.github.asmitmans.iotbackend.repository.DeviceModelRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import io.github.asmitmans.iotbackend.repository.LocationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final CompanyRepository companyRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final LocationRepository locationRepository;
    private final ApiKeyService apiKeyService;

    public DeviceService(DeviceRepository deviceRepository, CompanyRepository companyRepository,
                         DeviceModelRepository deviceModelRepository,
                         LocationRepository locationRepository,
                         ApiKeyService apiKeyService) {
        this.deviceRepository = deviceRepository;
        this.companyRepository = companyRepository;
        this.deviceModelRepository = deviceModelRepository;
        this.locationRepository = locationRepository;
        this.apiKeyService = apiKeyService;
    }

    public DeviceRegistrationResponse register(DeviceRegistrationRequest request) {

        String serialNumber;
        do {
            serialNumber = UUID.randomUUID().toString();
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
        response.setId(device.getId());
        response.setSerialNumber(device.getSerialNumber());
        response.setName(device.getName());
        response.setStatus(device.getStatus());

        return response;
    }

    public void enroll(String serialNumber, Integer companyId) {
        Device device = deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Device not " +
                                                                         "found"));

        if (!device.getStatus().equals("UNCLAIMED")) {
            throw new ConflictException("Device is not available for " +
                                                "enrollment");
        }

        Company company = companyRepository.findById(companyId)
                                           .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        device.setCompany(company);
        device.setStatus("PENDING");
        device.setClaimExpiresAt(Instant.now().plusSeconds(300));

        deviceRepository.save(device);
    }

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
      device.setCompany(null);
      device.setClaimExpiresAt(null);
      deviceRepository.save(device);
      throw new ConflictException("Enrollment window expired");
    }

    String apiKeyPlain = apiKeyService.generate();

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

}
