package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.device.DeviceCommandRequest;
import io.github.asmitmans.iotbackend.dto.device.DeviceCommandResponse;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.DeviceCommand;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.DeviceCommandRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeviceCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Long DEVICE_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long OTHER_ACCOUNT_ID = 99L;

    @Mock
    private DeviceCommandRepository deviceCommandRepository;

    @Mock
    private DeviceRepository deviceRepository;

    private DeviceCommandService service;

    @BeforeEach
    void setUp() {
        service = new DeviceCommandService(deviceCommandRepository, deviceRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ---------- queueCommand() ----------

    @Test
    void queueCommand_forOwnDevice_savesQueuedCommandAndMarksDevicePending() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        when(deviceRepository.findByIdForUpdate(DEVICE_ID)).thenReturn(Optional.of(device));
        when(deviceCommandRepository.findByDeviceIdAndStatusAndType(DEVICE_ID, "QUEUED", "REBOOT"))
                .thenReturn(List.of());

        service.queueCommand(DEVICE_ID, request("REBOOT", "{}"), ACCOUNT_ID);

        DeviceCommand saved = capturedSavedCommand();
        assertThat(saved.getDevice()).isSameAs(device);
        assertThat(saved.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(saved.getType()).isEqualTo("REBOOT");
        assertThat(saved.getPayload()).isEqualTo("{}");
        assertThat(saved.getStatus()).isEqualTo("QUEUED");
        assertThat(device.isCommandPending()).isTrue();
        verify(deviceRepository).save(device);
    }

    @Test
    void queueCommand_asAdmin_assignsTheDeviceAccountToTheCommand() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        when(deviceRepository.findByIdForUpdate(DEVICE_ID)).thenReturn(Optional.of(device));
        when(deviceCommandRepository.findByDeviceIdAndStatusAndType(DEVICE_ID, "QUEUED", "REBOOT"))
                .thenReturn(List.of());

        service.queueCommand(DEVICE_ID, request("REBOOT", null), null);

        assertThat(capturedSavedCommand().getAccountId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    void queueCommand_withQueuedCommandOfSameType_supersedesItAndQueuesTheNewOne() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        DeviceCommand previous = queuedCommand(device, "SET_TEMP", "{\"value\":20}");
        when(deviceRepository.findByIdForUpdate(DEVICE_ID)).thenReturn(Optional.of(device));
        when(deviceCommandRepository.findByDeviceIdAndStatusAndType(DEVICE_ID, "QUEUED", "SET_TEMP"))
                .thenReturn(List.of(previous));

        service.queueCommand(DEVICE_ID, request("SET_TEMP", "{\"value\":22}"), ACCOUNT_ID);

        assertThat(previous.getStatus()).isEqualTo("SUPERSEDED");
        DeviceCommand saved = capturedSavedCommand();
        assertThat(saved).isNotSameAs(previous);
        assertThat(saved.getStatus()).isEqualTo("QUEUED");
        assertThat(saved.getPayload()).isEqualTo("{\"value\":22}");
    }

    @Test
    void queueCommand_whenDeviceDoesNotExist_throwsNotFound() {
        when(deviceRepository.findByIdForUpdate(DEVICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.queueCommand(DEVICE_ID, request("REBOOT", null), ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found");

        verifyNoInteractions(deviceCommandRepository);
    }

    @Test
    void queueCommand_whenDeviceBelongsToAnotherAccount_throwsNotFound() {
        when(deviceRepository.findByIdForUpdate(DEVICE_ID)).thenReturn(Optional.of(deviceOfAccount(ACCOUNT_ID)));

        assertThatThrownBy(() -> service.queueCommand(DEVICE_ID, request("REBOOT", null), OTHER_ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found");

        verifyNoInteractions(deviceCommandRepository);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void queueCommand_asUserForDeviceWithoutAccount_throwsNotFound() {
        when(deviceRepository.findByIdForUpdate(DEVICE_ID)).thenReturn(Optional.of(deviceOfAccount(null)));

        assertThatThrownBy(() -> service.queueCommand(DEVICE_ID, request("REBOOT", null), ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found");

        verifyNoInteractions(deviceCommandRepository);
    }

    @Test
    void queueCommand_asAdminForDeviceWithoutAccount_throwsConflict() {
        when(deviceRepository.findByIdForUpdate(DEVICE_ID)).thenReturn(Optional.of(deviceOfAccount(null)));

        assertThatThrownBy(() -> service.queueCommand(DEVICE_ID, request("REBOOT", null), null))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Device is not enrolled to an account");

        verifyNoInteractions(deviceCommandRepository);
        verify(deviceRepository, never()).save(any());
    }

    // ---------- getNextCommand() ----------

    @Test
    void getNextCommand_withQueuedCommand_marksItDeliveredAndReturnsIt() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        device.setCommandPending(true);
        DeviceCommand command = queuedCommand(device, "REBOOT", "{}");
        command.setId(5L);
        when(deviceCommandRepository.findFirstByDeviceIdAndStatusOrderByQueuedAtAsc(DEVICE_ID, "QUEUED"))
                .thenReturn(Optional.of(command));
        when(deviceCommandRepository.existsByDeviceIdAndStatus(DEVICE_ID, "QUEUED")).thenReturn(false);

        Optional<DeviceCommandResponse> response = service.getNextCommand(device);

        assertThat(command.getStatus()).isEqualTo("DELIVERED");
        assertThat(command.getDeliveredAt()).isEqualTo(NOW);
        verify(deviceCommandRepository).save(command);
        assertThat(device.isCommandPending()).isFalse();
        verify(deviceRepository).save(device);
        assertThat(response).hasValueSatisfying(r -> {
            assertThat(r.getId()).isEqualTo(5L);
            assertThat(r.getType()).isEqualTo("REBOOT");
            assertThat(r.getPayload()).isEqualTo("{}");
        });
    }

    @Test
    void getNextCommand_whenMoreCommandsRemainQueued_keepsTheDevicePending() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        device.setCommandPending(true);
        when(deviceCommandRepository.findFirstByDeviceIdAndStatusOrderByQueuedAtAsc(DEVICE_ID, "QUEUED"))
                .thenReturn(Optional.of(queuedCommand(device, "REBOOT", null)));
        when(deviceCommandRepository.existsByDeviceIdAndStatus(DEVICE_ID, "QUEUED")).thenReturn(true);

        service.getNextCommand(device);

        assertThat(device.isCommandPending()).isTrue();
    }

    @Test
    void getNextCommand_withoutQueuedCommands_returnsEmptyAndSavesNothing() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        when(deviceCommandRepository.findFirstByDeviceIdAndStatusOrderByQueuedAtAsc(DEVICE_ID, "QUEUED"))
                .thenReturn(Optional.empty());

        Optional<DeviceCommandResponse> response = service.getNextCommand(device);

        assertThat(response).isEmpty();
        verify(deviceCommandRepository, never()).save(any());
        verify(deviceRepository, never()).save(any());
    }

    // ---------- helpers ----------

    private Device deviceOfAccount(Long accountId) {
        Device device = new Device();
        device.setId(DEVICE_ID);
        if (accountId != null) {
            Account account = new Account();
            account.setId(accountId);
            device.setAccount(account);
        }
        return device;
    }

    private DeviceCommand queuedCommand(Device device, String type, String payload) {
        DeviceCommand command = new DeviceCommand();
        command.setDevice(device);
        command.setType(type);
        command.setPayload(payload);
        command.setStatus("QUEUED");
        return command;
    }

    private DeviceCommandRequest request(String type, String payload) {
        DeviceCommandRequest request = new DeviceCommandRequest();
        request.setType(type);
        request.setPayload(payload);
        return request;
    }

    private DeviceCommand capturedSavedCommand() {
        ArgumentCaptor<DeviceCommand> saved = ArgumentCaptor.forClass(DeviceCommand.class);
        verify(deviceCommandRepository).save(saved.capture());
        return saved.getValue();
    }
}