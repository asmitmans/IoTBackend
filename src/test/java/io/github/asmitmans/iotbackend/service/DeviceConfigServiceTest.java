package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.device.DeviceConfigRequest;
import io.github.asmitmans.iotbackend.dto.device.DeviceConfigResponse;
import io.github.asmitmans.iotbackend.dto.device.DeviceConfigStatusResponse;
import io.github.asmitmans.iotbackend.dto.device.DeviceStateRequest;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.DeviceConfig;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.DeviceConfigRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeviceConfigServiceTest {

    private static final Long DEVICE_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long OTHER_ACCOUNT_ID = 99L;

    @Mock
    private DeviceConfigRepository deviceConfigRepository;

    @Mock
    private DeviceRepository deviceRepository;

    private DeviceConfigService service;

    private long nextConfigId = 100L;

    @BeforeEach
    void setUp() {
        service = new DeviceConfigService(deviceConfigRepository, deviceRepository);
    }

    // ---------- reportState() ----------

    @Test
    void reportState_whenReportedEqualsDesired_marksSyncedAndClearsPending() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        device.setConfigPending(true);
        DeviceConfig interval = config(device, "interval", "60", null, "PENDING");
        when(deviceConfigRepository.findByDeviceIdAndKey(DEVICE_ID, "interval")).thenReturn(Optional.of(interval));
        when(deviceConfigRepository.findByDeviceId(DEVICE_ID)).thenReturn(List.of(interval));

        service.reportState(device, stateRequest(Map.of("interval", "60")));

        assertThat(interval.getReportedValue()).isEqualTo("60");
        assertThat(interval.getStatus()).isEqualTo("SYNCED");
        verify(deviceConfigRepository).save(interval);
        assertThat(device.isConfigPending()).isFalse();
        verify(deviceRepository).save(device);
    }

    @Test
    void reportState_whenReportedDiffersFromDesired_marksConflict() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        DeviceConfig interval = config(device, "interval", "60", null, "PENDING");
        when(deviceConfigRepository.findByDeviceIdAndKey(DEVICE_ID, "interval")).thenReturn(Optional.of(interval));
        when(deviceConfigRepository.findByDeviceId(DEVICE_ID)).thenReturn(List.of(interval));

        service.reportState(device, stateRequest(Map.of("interval", "30")));

        assertThat(interval.getReportedValue()).isEqualTo("30");
        assertThat(interval.getStatus()).isEqualTo("CONFLICT");
    }

    @Test
    void reportState_forKeyWithoutDesiredValue_createsItAsReported() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        when(deviceConfigRepository.findByDeviceIdAndKey(DEVICE_ID, "firmware")).thenReturn(Optional.empty());
        when(deviceConfigRepository.findByDeviceId(DEVICE_ID)).thenReturn(List.of());

        service.reportState(device, stateRequest(Map.of("firmware", "1.2.0")));

        DeviceConfig saved = capturedSavedConfig();
        assertThat(saved.getDevice()).isSameAs(device);
        assertThat(saved.getKey()).isEqualTo("firmware");
        assertThat(saved.getDesiredValue()).isNull();
        assertThat(saved.getReportedValue()).isEqualTo("1.2.0");
        assertThat(saved.getStatus()).isEqualTo("REPORTED");
        assertThat(saved.getConfigVersion()).isEqualTo(1L);
    }

    @Test
    void reportState_whenAnotherKeyIsStillPending_keepsTheDevicePending() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        device.setConfigPending(true);
        DeviceConfig interval = config(device, "interval", "60", null, "PENDING");
        DeviceConfig threshold = config(device, "threshold", "25", null, "PENDING");
        when(deviceConfigRepository.findByDeviceIdAndKey(DEVICE_ID, "interval")).thenReturn(Optional.of(interval));
        when(deviceConfigRepository.findByDeviceId(DEVICE_ID)).thenReturn(List.of(interval, threshold));

        service.reportState(device, stateRequest(Map.of("interval", "60")));

        assertThat(interval.getStatus()).isEqualTo("SYNCED");
        assertThat(device.isConfigPending()).isTrue();
    }

    // ---------- setDesiredConfig() ----------

    @Test
    void setDesiredConfig_forNewKey_createsItPendingAndMarksDevicePending() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(deviceConfigRepository.findByDeviceIdAndKey(DEVICE_ID, "interval")).thenReturn(Optional.empty());

        service.setDesiredConfig(DEVICE_ID, desiredRequest(Map.of("interval", "60")), ACCOUNT_ID);

        DeviceConfig saved = capturedSavedConfig();
        assertThat(saved.getDevice()).isSameAs(device);
        assertThat(saved.getKey()).isEqualTo("interval");
        assertThat(saved.getDesiredValue()).isEqualTo("60");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getConfigVersion()).isEqualTo(1L);
        assertThat(device.isConfigPending()).isTrue();
        verify(deviceRepository).save(device);
    }

    @Test
    void setDesiredConfig_forExistingKey_updatesDesiredAndReturnsItToPending() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        DeviceConfig interval = config(device, "interval", "60", "60", "SYNCED");
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(deviceConfigRepository.findByDeviceIdAndKey(DEVICE_ID, "interval")).thenReturn(Optional.of(interval));

        service.setDesiredConfig(DEVICE_ID, desiredRequest(Map.of("interval", "30")), ACCOUNT_ID);

        assertThat(interval.getDesiredValue()).isEqualTo("30");
        assertThat(interval.getReportedValue()).isEqualTo("60");
        assertThat(interval.getStatus()).isEqualTo("PENDING");
        verify(deviceConfigRepository).save(interval);
    }

    @Test
    void setDesiredConfig_asAdminForDeviceWithoutAccount_isAllowed() {
        Device device = deviceOfAccount(null);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(deviceConfigRepository.findByDeviceIdAndKey(DEVICE_ID, "interval")).thenReturn(Optional.empty());

        service.setDesiredConfig(DEVICE_ID, desiredRequest(Map.of("interval", "60")), null);

        assertThat(capturedSavedConfig().getStatus()).isEqualTo("PENDING");
        assertThat(device.isConfigPending()).isTrue();
    }

    @Test
    void setDesiredConfig_whenDeviceBelongsToAnotherAccount_throwsNotFound() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(deviceOfAccount(ACCOUNT_ID)));

        assertThatThrownBy(() -> service.setDesiredConfig(DEVICE_ID, desiredRequest(Map.of("interval", "60")), OTHER_ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found");

        verifyNoInteractions(deviceConfigRepository);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void setDesiredConfig_whenDeviceDoesNotExist_throwsNotFound() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setDesiredConfig(DEVICE_ID, desiredRequest(Map.of("interval", "60")), ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found");

        verifyNoInteractions(deviceConfigRepository);
    }

    // ---------- getConfig() ----------

    @Test
    void getConfig_returnsOnlyKeysWithADesiredValue() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        when(deviceConfigRepository.findByDeviceId(DEVICE_ID)).thenReturn(List.of(
                config(device, "interval", "60", null, "PENDING"),
                config(device, "firmware", null, "1.2.0", "REPORTED")));

        DeviceConfigResponse response = service.getConfig(device);

        assertThat(response.getDesired()).containsExactly(Map.entry("interval", "60"));
    }

    // ---------- getConfigStatus() ----------

    @Test
    void getConfigStatus_returnsEveryKeySortedByKey() {
        Device device = deviceOfAccount(ACCOUNT_ID);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(deviceConfigRepository.findByDeviceId(DEVICE_ID)).thenReturn(List.of(
                config(device, "threshold", "25", "20", "CONFLICT"),
                config(device, "firmware", null, "1.2.0", "REPORTED"),
                config(device, "interval", "60", "60", "SYNCED")));

        List<DeviceConfigStatusResponse> status = service.getConfigStatus(DEVICE_ID, ACCOUNT_ID);

        assertThat(status)
                .extracting(DeviceConfigStatusResponse::getKey,
                        DeviceConfigStatusResponse::getDesiredValue,
                        DeviceConfigStatusResponse::getReportedValue,
                        DeviceConfigStatusResponse::getStatus)
                .containsExactly(
                        tuple("firmware", null, "1.2.0", "REPORTED"),
                        tuple("interval", "60", "60", "SYNCED"),
                        tuple("threshold", "25", "20", "CONFLICT"));
    }

    @Test
    void getConfigStatus_forDeviceWithoutConfig_returnsEmptyList() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(deviceOfAccount(ACCOUNT_ID)));
        when(deviceConfigRepository.findByDeviceId(DEVICE_ID)).thenReturn(List.of());

        assertThat(service.getConfigStatus(DEVICE_ID, ACCOUNT_ID)).isEmpty();
    }

    @Test
    void getConfigStatus_whenDeviceBelongsToAnotherAccount_throwsNotFound() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(deviceOfAccount(ACCOUNT_ID)));

        assertThatThrownBy(() -> service.getConfigStatus(DEVICE_ID, OTHER_ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found");

        verifyNoInteractions(deviceConfigRepository);
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

    private DeviceConfig config(Device device, String key, String desired, String reported, String status) {
        DeviceConfig config = new DeviceConfig();
        config.setId(nextConfigId++);
        config.setDevice(device);
        config.setKey(key);
        config.setConfigVersion(1L);
        config.setDesiredValue(desired);
        config.setReportedValue(reported);
        config.setStatus(status);
        return config;
    }

    private DeviceStateRequest stateRequest(Map<String, String> reported) {
        DeviceStateRequest request = new DeviceStateRequest();
        request.setReported(reported);
        return request;
    }

    private DeviceConfigRequest desiredRequest(Map<String, String> desired) {
        DeviceConfigRequest request = new DeviceConfigRequest();
        request.setDesired(desired);
        return request;
    }

    private DeviceConfig capturedSavedConfig() {
        ArgumentCaptor<DeviceConfig> saved = ArgumentCaptor.forClass(DeviceConfig.class);
        verify(deviceConfigRepository).save(saved.capture());
        return saved.getValue();
    }
}