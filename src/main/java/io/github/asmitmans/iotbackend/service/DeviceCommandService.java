package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.device.CreateCommandRequest;
import io.github.asmitmans.iotbackend.dto.device.DeviceCommandResponse;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.DeviceCommand;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.DeviceCommandRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class DeviceCommandService {

    private final DeviceCommandRepository deviceCommandRepository;
    private final DeviceRepository deviceRepository;

    public DeviceCommandService(DeviceCommandRepository deviceCommandRepository, DeviceRepository deviceRepository) {
        this.deviceCommandRepository = deviceCommandRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public void createCommand(Long deviceId, CreateCommandRequest request,
                              Long accountId) {
        Device device = deviceRepository.findById(deviceId)
                                        .filter(d -> accountId == null ||
                                                (d.getAccount() != null &&
                                                        d.getAccount().getId().longValue() == accountId))
                                        .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        Long effectiveAccountId = accountId != null ?
                accountId : device.getAccount().getId().longValue();

        if (deviceCommandRepository.existsByDeviceIdAndStatusAndType(device.getId(), "QUEUED", request.getType())) {
            throw new ConflictException("Command of type '" + request.getType() +
                                                "' already queued for this device");
        }



        DeviceCommand command = new DeviceCommand();
        command.setDevice(device);
        command.setAccountId(effectiveAccountId);
        command.setType(request.getType());
        command.setPayload(request.getPayload());
        command.setStatus("QUEUED");

        deviceCommandRepository.save(command);

        device.setCommandPending(true);
        deviceRepository.save(device);
    }

    @Transactional
    public Optional<DeviceCommandResponse> getNextCommand(Device device) {
        return deviceCommandRepository
                .findFirstByDeviceIdAndStatusOrderByQueuedAtAsc(
                        device.getId(), "QUEUED")
                .map(command -> {
                    command.setStatus("DELIVERED");
                    command.setDeliveredAt(Instant.now());
                    deviceCommandRepository.save(command);

                    boolean stillPending = deviceCommandRepository
                            .existsByDeviceIdAndStatus(device.getId(), "QUEUED");
                    device.setCommandPending(stillPending);
                    deviceRepository.save(device);

                    DeviceCommandResponse response = new DeviceCommandResponse();
                    response.setId(command.getId());
                    response.setType(command.getType());
                    response.setPayload(command.getPayload());
                    return response;
                });
    }
}