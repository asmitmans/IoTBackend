package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.device.AdminConfigRequest;
import io.github.asmitmans.iotbackend.dto.device.DeviceConfigResponse;
import io.github.asmitmans.iotbackend.dto.device.DeviceStateRequest;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.DeviceConfig;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.DeviceConfigRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeviceConfigService {

    private final DeviceConfigRepository deviceConfigRepository;
    private final DeviceRepository deviceRepository;

    public DeviceConfigService(DeviceConfigRepository deviceConfigRepository, DeviceRepository deviceRepository) {
        this.deviceConfigRepository = deviceConfigRepository;
        this.deviceRepository = deviceRepository;
    }

    // GET /config -> device read desired values
    public DeviceConfigResponse getConfig(Device device) {
        Map<String, String> desired = deviceConfigRepository
                .findByDeviceId(device.getId())
                .stream()
                .filter(c -> c.getDesiredValue() != null)
                .collect(Collectors.toMap(
                        DeviceConfig::getKey,
                        DeviceConfig::getDesiredValue));

        DeviceConfigResponse respose = new DeviceConfigResponse();
        respose.setDesired(desired);
        return respose;
    }

    // POST /state -> device report reported values
    @Transactional
    public void reportState(Device device, DeviceStateRequest request) {
        request.getReported().forEach((key, value) -> {
            DeviceConfig config = deviceConfigRepository
                    .findByDeviceIdAndKey(device.getId(), key)
                    .orElseGet(() -> {
                        DeviceConfig c = new DeviceConfig();
                        c.setDevice(device);
                        c.setKey(key);
                        c.setConfigVersion(1L);
                        return c;
                    });

            config.setReportedValue(value);
            config.setStatus(
                    value.equals(config.getDesiredValue()) ? "SYNCED" : "CONFLICT"
            );
            deviceConfigRepository.save(config);
        });

        // update config_pending flag
        boolean hasPending = deviceConfigRepository
                .findByDeviceId(device.getId())
                .stream()
                .anyMatch(c -> "PENDING".equals(c.getStatus()));

        device.setConfigPending(hasPending);
        deviceRepository.save(device);
    }

    // PUT /config -> admin set desired values
    @Transactional
    public void setDesiredConfig(Long deviceId, AdminConfigRequest request, Long companyId) {
        Device device = deviceRepository.findById(deviceId)
                                        .filter(d -> d.getCompany().getId().longValue() == companyId)
                                        .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        request.getDesired().forEach((key, value) -> {
            DeviceConfig config = deviceConfigRepository
                    .findByDeviceIdAndKey(deviceId, key)
                    .orElseGet(() -> {
                        DeviceConfig c = new DeviceConfig();
                        c.setDevice(device);
                        c.setKey(key);
                        c.setConfigVersion(1L);
                        return c;
                    });

            config.setDesiredValue(value);
            config.setStatus("PENDING");
            deviceConfigRepository.save(config);
        });

        device.setConfigPending(true);
        deviceRepository.save(device);
    }

}
