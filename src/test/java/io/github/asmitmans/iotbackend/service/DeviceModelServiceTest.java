package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.entity.DeviceModel;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.repository.CommandTypeRepository;
import io.github.asmitmans.iotbackend.repository.DeviceModelRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import io.github.asmitmans.iotbackend.repository.MeasurementTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeviceModelServiceTest {

    private static final Long ID = 7L;

    @Mock
    private DeviceModelRepository deviceModelRepository;

    @Mock
    private MeasurementTypeRepository measurementTypeRepository;

    @Mock
    private CommandTypeRepository commandTypeRepository;

    @Mock
    private DeviceRepository deviceRepository;

    private DeviceModelService service;

    private DeviceModel entity;

    @BeforeEach
    void setUp() {
        service = new DeviceModelService(deviceModelRepository, measurementTypeRepository, commandTypeRepository,
                deviceRepository);
        entity = new DeviceModel();
        entity.setId(ID);
    }

    // ---------- delete() ----------

    @Test
    void delete_whenNotInUse_deletesIt() {
        when(deviceModelRepository.findById(ID)).thenReturn(Optional.of(entity));

        service.delete(ID);

        verify(deviceModelRepository).delete(entity);
    }

    @Test
    void delete_whenInUse_throwsConflictAndDoesNotDelete() {
        when(deviceModelRepository.findById(ID)).thenReturn(Optional.of(entity));
        when(deviceRepository.existsByDeviceModelId(ID)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(ID)).isInstanceOf(ConflictException.class)
            .hasMessage("Device model is used by devices; reassign them first");

        verify(deviceModelRepository, never()).delete(any());
    }

}