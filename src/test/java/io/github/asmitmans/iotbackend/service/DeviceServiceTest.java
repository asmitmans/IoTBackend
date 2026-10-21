package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.device.DeviceEnrollResponse;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.DeviceModelRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import io.github.asmitmans.iotbackend.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeviceServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String SERIAL = "ABCDEFGHJKLMNPQR";

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DeviceModelRepository deviceModelRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ApiKeyService apiKeyService;

    private DeviceService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new DeviceService(deviceRepository, accountRepository, deviceModelRepository,
                locationRepository, apiKeyService, fixedClock);
    }

    // ---------- enroll() ----------

    @Test
    void enroll_fromUnclaimed_setsPendingWithNameAccountAndFiveMinuteWindow() {
        Device device = deviceWithStatus("UNCLAIMED");
        Account account = new Account();
        UUID accountPublicId = UUID.randomUUID();
        when(deviceRepository.findBySerialNumberForUpdate(SERIAL)).thenReturn(Optional.of(device));
        when(accountRepository.findByPublicId(accountPublicId)).thenReturn(Optional.of(account));

        DeviceEnrollResponse response = service.enroll(SERIAL, accountPublicId, "Sensor 1");

        assertThat(device.getStatus()).isEqualTo("PENDING");
        assertThat(device.getName()).isEqualTo("Sensor 1");
        assertThat(device.getAccount()).isSameAs(account);
        assertThat(device.getClaimExpiresAt()).isEqualTo(NOW.plusSeconds(300));
        verify(deviceRepository).save(device);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getSerialNumber()).isEqualTo(SERIAL);
        assertThat(response.getName()).isEqualTo("Sensor 1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "ACTIVE", "INACTIVE"})
    void enroll_whenDeviceIsNotUnclaimed_throwsConflictAndDoesNotSave(String status) {
        Device device = deviceWithStatus(status);
        when(deviceRepository.findBySerialNumberForUpdate(SERIAL)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.enroll(SERIAL, UUID.randomUUID(), "Sensor 1"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Device is not available for enrollment");

        verifyNoInteractions(accountRepository);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void enroll_whenAccountDoesNotExist_throwsNotFoundAndDoesNotSave() {
        Device device = deviceWithStatus("UNCLAIMED");
        UUID accountPublicId = UUID.randomUUID();
        when(deviceRepository.findBySerialNumberForUpdate(SERIAL)).thenReturn(Optional.of(device));
        when(accountRepository.findByPublicId(accountPublicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enroll(SERIAL, accountPublicId, "Sensor 1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found");

        assertThat(device.getStatus()).isEqualTo("UNCLAIMED");
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void enroll_whenDeviceDoesNotExist_throwsNotFound() {
        when(deviceRepository.findBySerialNumberForUpdate(SERIAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enroll(SERIAL, UUID.randomUUID(), "Sensor 1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found");

        verifyNoInteractions(accountRepository);
    }

    // ---------- helpers ----------

    private Device deviceWithStatus(String status) {
        Device device = new Device();
        device.setSerialNumber(SERIAL);
        device.setStatus(status);
        return device;
    }
}