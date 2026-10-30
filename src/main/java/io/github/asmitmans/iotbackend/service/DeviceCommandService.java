package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.device.DeviceCommandRequest;
import io.github.asmitmans.iotbackend.dto.device.DeviceCommandResponse;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.DeviceCommand;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.DeviceCommandRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceCommandService {

    private final DeviceCommandRepository deviceCommandRepository;
    private final DeviceRepository deviceRepository;
    private final Clock clock;

    public DeviceCommandService(DeviceCommandRepository deviceCommandRepository,
                                DeviceRepository deviceRepository,
                                Clock clock) {
        this.deviceCommandRepository = deviceCommandRepository;
        this.deviceRepository = deviceRepository;
        this.clock = clock;
    }

    @Transactional
    public void queueCommand(Long deviceId, DeviceCommandRequest request,
                                  Long accountId) {
        Device device = deviceRepository.findByIdForUpdate(deviceId)
                                        .filter(d -> accountId == null ||
                                                (d.getAccount() != null &&
                                                        d.getAccount().getId().longValue() == accountId))
                                        .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (device.getAccount() == null) {
            throw new ConflictException("Device is not enrolled to an account");
        }

        Long effectiveAccountId = accountId != null ?
                accountId : device.getAccount().getId().longValue();

        List<DeviceCommand> queuedOfSameType = deviceCommandRepository
                .findByDeviceIdAndStatusAndType(device.getId(), "QUEUED", request.getType());
        queuedOfSameType.forEach(previous -> previous.setStatus("SUPERSEDED"));
        deviceCommandRepository.saveAllAndFlush(queuedOfSameType);

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
                    command.setDeliveredAt(Instant.now(clock));
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