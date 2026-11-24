package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.entity.CommandType;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.repository.CommandTypeRepository;
import io.github.asmitmans.iotbackend.repository.DeviceModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommandTypeServiceTest {

    private static final Long ID = 7L;

    @Mock
    private CommandTypeRepository commandTypeRepository;

@Mock
private DeviceModelRepository deviceModelRepository;

private CommandTypeService service;

private CommandType entity;

@BeforeEach
void setUp() {
    service = new CommandTypeService(commandTypeRepository, deviceModelRepository);
    entity = new CommandType();
    entity.setId(ID);
}

// ---------- delete() ----------

@Test
void delete_whenNotInUse_deletesIt() {
    when(commandTypeRepository.findById(ID)).thenReturn(Optional.of(entity));

    service.delete(ID);

    verify(commandTypeRepository).delete(entity);
}

@Test
void delete_whenInUse_throwsConflictAndDoesNotDelete() {
    when(commandTypeRepository.findById(ID)).thenReturn(Optional.of(entity));
    when(deviceModelRepository.existsByCommandTypesId(ID)).thenReturn(true);

    assertThatThrownBy(() -> service.delete(ID))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Command type is used by device models; remove it from them first");

    verify(commandTypeRepository, never()).delete(any());
}
}
