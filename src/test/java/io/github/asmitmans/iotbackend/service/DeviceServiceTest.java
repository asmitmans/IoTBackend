package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.device.DeviceClaimResponse;
import io.github.asmitmans.iotbackend.dto.device.DeviceEnrollResponse;
import io.github.asmitmans.iotbackend.dto.device.DeviceRegistrationRequest;
import io.github.asmitmans.iotbackend.dto.device.DeviceRegistrationResponse;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.DeviceModel;
import io.github.asmitmans.iotbackend.entity.Location;
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
import org.mockito.ArgumentCaptor;
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
    private static final Long DEVICE_ID = 42L;
    private static final Long MODEL_ID = 7L;
    private static final Long LOCATION_ID = 3L;
    private static final String SERIAL_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";


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

    // ---------- claim() ----------

    @Test
    void claim_fromPendingWithinWindow_activatesStoresKeyHashAndReturnsPlainKey() {
        Device device = pendingDeviceExpiringAt(NOW.plusSeconds(60));
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.of(device));
        givenApiKeyGenerated("iotdev_PLAIN", "HASH", "iotdev_PL");

        DeviceClaimResponse response = service.claim(SERIAL);

        assertThat(device.getStatus()).isEqualTo("ACTIVE");
        assertThat(device.getApiKeyHash()).isEqualTo("HASH");
        assertThat(device.getApiKeyPrefix()).isEqualTo("iotdev_PL");
        assertThat(device.getClaimedAt()).isEqualTo(NOW);
        assertThat(device.getClaimExpiresAt()).isNull();
        verify(deviceRepository).save(device);
        assertThat(response.getApiKeyPlain()).isEqualTo("iotdev_PLAIN");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getSerialNumber()).isEqualTo(SERIAL);
    }

    @Test
    void claim_exactlyAtExpiry_isStillAccepted() {
        Device device = pendingDeviceExpiringAt(NOW);
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.of(device));
        givenApiKeyGenerated("iotdev_PLAIN", "HASH", "iotdev_PL");

        service.claim(SERIAL);

        assertThat(device.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void claim_whenWindowExpired_resetsToUnclaimedSavesAndThrowsConflict() {
        Device device = pendingDeviceExpiringAt(NOW.minusSeconds(1));
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.claim(SERIAL))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Enrollment window expired");

        assertThat(device.getStatus()).isEqualTo("UNCLAIMED");
        assertThat(device.getAccount()).isNull();
        assertThat(device.getClaimExpiresAt()).isNull();
        verify(deviceRepository).save(device);
        verifyNoInteractions(apiKeyService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNCLAIMED", "ACTIVE", "INACTIVE"})
    void claim_whenDeviceIsNotPending_throwsConflictAndDoesNotSave(String status) {
        Device device = deviceWithStatus(status);
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.claim(SERIAL))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Device is not pending enrollment");

        verifyNoInteractions(apiKeyService);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void claim_whenDeviceDoesNotExist_throwsNotFound() {
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.claim(SERIAL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found");

        verifyNoInteractions(apiKeyService);
    }

    // ---------- updateStatus() ----------

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "INACTIVE"})
    void updateStatus_withAllowedStatus_setsStatusAndSaves(String newStatus) {
        Device device = deviceWithStatus("ACTIVE");
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));

        service.updateStatus(DEVICE_ID, newStatus);

        assertThat(device.getStatus()).isEqualTo(newStatus);
        verify(deviceRepository).save(device);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNCLAIMED", "PENDING", "active", "UNKNOWN"})
    void updateStatus_withDisallowedStatus_throwsIllegalArgumentAndDoesNotSave(String newStatus) {
        Device device = deviceWithStatus("ACTIVE");
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.updateStatus(DEVICE_ID, newStatus))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid status: " + newStatus);

        assertThat(device.getStatus()).isEqualTo("ACTIVE");
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void updateStatus_whenDeviceDoesNotExist_throwsNotFoundAndDoesNotSave() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(DEVICE_ID, "ACTIVE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found");

        verify(deviceRepository, never()).save(any());
    }

    // ---------- register() ----------

    @Test
    void register_withModelAndNoLocation_savesUnclaimedDeviceWithGeneratedSerial() {
        DeviceModel model = new DeviceModel();
        when(deviceModelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));
        givenSaveReturnsTheSameDevice();

        DeviceRegistrationResponse response = service.register(registrationRequest(MODEL_ID, null));

        Device saved = capturedSavedDevice();
        assertThat(saved.getSerialNumber()).matches("[" + SERIAL_ALPHABET + "]{16}");
        assertThat(saved.getStatus()).isEqualTo("UNCLAIMED");
        assertThat(saved.getDeviceModel()).isSameAs(model);
        assertThat(saved.getLocation()).isNull();
        verifyNoInteractions(locationRepository);
        assertThat(response.getSerialNumber()).isEqualTo(saved.getSerialNumber());
        assertThat(response.getStatus()).isEqualTo("UNCLAIMED");
    }

    @Test
    void register_withLocation_assignsTheLocation() {
        Location location = new Location();
        when(deviceModelRepository.findById(MODEL_ID)).thenReturn(Optional.of(new DeviceModel()));
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location));
        givenSaveReturnsTheSameDevice();

        service.register(registrationRequest(MODEL_ID, LOCATION_ID));

        assertThat(capturedSavedDevice().getLocation()).isSameAs(location);
    }

    @Test
    void register_whenGeneratedSerialCollides_retriesUntilItIsFree() {
        when(deviceRepository.findBySerialNumber(anyString()))
                .thenReturn(Optional.of(new Device()))
                .thenReturn(Optional.empty());
        when(deviceModelRepository.findById(MODEL_ID)).thenReturn(Optional.of(new DeviceModel()));
        givenSaveReturnsTheSameDevice();

        service.register(registrationRequest(MODEL_ID, null));

        ArgumentCaptor<String> checkedSerials = ArgumentCaptor.forClass(String.class);
        verify(deviceRepository, times(2)).findBySerialNumber(checkedSerials.capture());
        assertThat(capturedSavedDevice().getSerialNumber()).isEqualTo(checkedSerials.getAllValues().get(1));
    }

    @Test
    void register_whenModelDoesNotExist_throwsNotFoundAndDoesNotSave() {
        when(deviceModelRepository.findById(MODEL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(registrationRequest(MODEL_ID, LOCATION_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device model not found");

        verifyNoInteractions(locationRepository);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void register_whenLocationDoesNotExist_throwsNotFoundAndDoesNotSave() {
        when(deviceModelRepository.findById(MODEL_ID)).thenReturn(Optional.of(new DeviceModel()));
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(registrationRequest(MODEL_ID, LOCATION_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Location not found");

        verify(deviceRepository, never()).save(any());
    }

    // ---------- helpers ----------

    private Device deviceWithStatus(String status) {
        Device device = new Device();
        device.setSerialNumber(SERIAL);
        device.setStatus(status);
        return device;
    }

    private Device pendingDeviceExpiringAt(Instant claimExpiresAt) {
        Device device = deviceWithStatus("PENDING");
        device.setAccount(new Account());
        device.setClaimExpiresAt(claimExpiresAt);
        return device;
    }

    private void givenApiKeyGenerated(String plain, String hash, String prefix) {
        when(apiKeyService.generate("iotdev_")).thenReturn(plain);
        when(apiKeyService.hash(plain)).thenReturn(hash);
        when(apiKeyService.extractPrefix(plain)).thenReturn(prefix);
    }

    private DeviceRegistrationRequest registrationRequest(Long modelId, Long locationId) {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setDeviceModelId(modelId);
        request.setLocationId(locationId);
        return request;
    }

    private void givenSaveReturnsTheSameDevice() {
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Device capturedSavedDevice() {
        ArgumentCaptor<Device> saved = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository).save(saved.capture());
        return saved.getValue();
    }
}