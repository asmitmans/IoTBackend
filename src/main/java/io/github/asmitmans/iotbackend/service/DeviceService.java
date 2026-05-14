package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.device.DeviceRegistrationRequest;
import io.github.asmitmans.iotbackend.dto.device.DeviceRegistrationResponse;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.DeviceModel;
import io.github.asmitmans.iotbackend.entity.Location;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.DeviceModelRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import io.github.asmitmans.iotbackend.repository.LocationRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final LocationRepository locationRepository;
    private final ApiKeyService apiKeyService;

    public DeviceService(DeviceRepository deviceRepository,
                         DeviceModelRepository deviceModelRepository,
                         LocationRepository locationRepository,
                         ApiKeyService apiKeyService) {
        this.deviceRepository = deviceRepository;
        this.deviceModelRepository = deviceModelRepository;
        this.locationRepository = locationRepository;
        this.apiKeyService = apiKeyService;
    }

    public DeviceRegistrationResponse register(DeviceRegistrationRequest request) {
        if (deviceRepository.findBySerialNumber(request.getSerialNumber()).isPresent()) {
            throw new ConflictException("Serial number already registered");
        }

        DeviceModel model = deviceModelRepository.findById(request.getDeviceModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Device model not found"));

        Location location = null;
        if (request.getLocationId() != null) {
            location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
        }

        Device device = new Device();
        device.setSerialNumber(request.getSerialNumber());
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
}