package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.entity.MeasurementType;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.repository.DeviceModelRepository;
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
public class MeasurementTypeServiceTest {

    private static final Long ID = 7L;

    @Mock
    private MeasurementTypeRepository measurementTypeRepository;

    @Mock
    private DeviceModelRepository deviceModelRepository;

    private MeasurementTypeService service;

    private MeasurementType entity;

    @BeforeEach
    void setUp() {
        service = new MeasurementTypeService(measurementTypeRepository, deviceModelRepository);
        entity = new MeasurementType();
        entity.setId(ID);
    }

    // ---------- delete() ----------

    @Test
    void delete_whenNotInUse_deletesIt() {
        when(measurementTypeRepository.findById(ID)).thenReturn(Optional.of(entity));

        service.delete(ID);

        verify(measurementTypeRepository).delete(entity);
    }

    @Test
    void delete_whenInUse_throwsConflictAndDoesNotDelete() {
        when(measurementTypeRepository.findById(ID)).thenReturn(Optional.of(entity));
        when(deviceModelRepository.existsByMeasurementTypesId(ID)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Measurement type is used by device models; remove it from them first");

        verify(measurementTypeRepository, never()).delete(any());
    }
}