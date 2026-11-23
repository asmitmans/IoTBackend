package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.location.LocationRequest;
import io.github.asmitmans.iotbackend.entity.Account;
import io.github.asmitmans.iotbackend.entity.Location;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.AccountRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import io.github.asmitmans.iotbackend.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LocationServiceTest {

    private static final Long ACCOUNT_ID = 10L;

    private static final UUID BUILDING_ID = UUID.randomUUID();
    private static final UUID FLOOR_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID RACK_ID = UUID.randomUUID();
    private static final UUID WAREHOUSE_ID = UUID.randomUUID();

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DeviceRepository deviceRepository;

    private LocationService service;

    private Account account;

    // Building → Floor → Room → Rack, and a separate root: Warehouse
    private Location building;
    private Location floor;
    private Location room;
    private Location rack;
    private Location warehouse;

    @BeforeEach
    void setUp() {
        service = new LocationService(locationRepository, accountRepository, deviceRepository);

        account = new Account();
        building = location(1L, "Building", null);
        floor = location(2L, "Floor 1", building);
        room = location(3L, "Room A", floor);
        rack = location(4L, "Rack 1", room);
        warehouse = location(5L, "Warehouse", null);
    }

    // ---------- update(): cycles ----------

    @Test
    void update_movingUnderItself_throwsConflictAndDoesNotSave() {
        givenAccountLocked();
        givenLocationExists(ROOM_ID, room);
        givenTheAccountTree();

        assertThatThrownBy(() -> service.update(ROOM_ID, request("Room A", ROOM_ID), ACCOUNT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Location cannot be moved under itself or its descendants");

        assertThat(room.getParent()).isSameAs(floor);
        verify(locationRepository, never()).save(any());
    }

    @Test
    void update_movingUnderAnIndirectDescendant_throwsConflictAndDoesNotSave() {
        givenAccountLocked();
        givenLocationExists(BUILDING_ID, building);
        givenLocationExists(RACK_ID, rack);
        givenTheAccountTree();

        assertThatThrownBy(() -> service.update(BUILDING_ID, request("Building", RACK_ID), ACCOUNT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Location cannot be moved under itself or its descendants");

        assertThat(building.getParent()).isNull();
        verify(locationRepository, never()).save(any());
    }

    @Test
    void update_movingUpUnderAnAncestor_isAllowed() {
        givenAccountLocked();
        givenLocationExists(ROOM_ID, room);
        givenLocationExists(BUILDING_ID, building);
        givenTheAccountTree();
        givenSaveReturnsTheSameLocation();

        service.update(ROOM_ID, request("Room A", BUILDING_ID), ACCOUNT_ID);

        assertThat(capturedSavedLocation().getParent()).isSameAs(building);
    }

    @Test
    void update_movingUnderAnotherBranch_savesTheNewParent() {
        givenAccountLocked();
        givenLocationExists(ROOM_ID, room);
        givenLocationExists(WAREHOUSE_ID, warehouse);
        givenTheAccountTree();
        givenSaveReturnsTheSameLocation();

        service.update(ROOM_ID, request("Room A", WAREHOUSE_ID), ACCOUNT_ID);

        assertThat(capturedSavedLocation().getParent()).isSameAs(warehouse);
    }

    @Test
    void update_whenStoredDataAlreadyHasACycle_stillTerminates() {
        floor.setParent(rack); // corrupted data: Floor → Room → Rack → Floor
        givenAccountLocked();
        givenLocationExists(ROOM_ID, room);
        givenLocationExists(WAREHOUSE_ID, warehouse);
        givenTheAccountTree();
        givenSaveReturnsTheSameLocation();

        service.update(ROOM_ID, request("Room A", WAREHOUSE_ID), ACCOUNT_ID);

        assertThat(capturedSavedLocation().getParent()).isSameAs(warehouse);
    }

    @Test
    void update_toRoot_skipsTheCycleCheck() {
        givenAccountLocked();
        givenLocationExists(ROOM_ID, room);
        givenSaveReturnsTheSameLocation();

        service.update(ROOM_ID, request("Room A", null), ACCOUNT_ID);

        assertThat(capturedSavedLocation().getParent()).isNull();
        verify(locationRepository, never()).findByAccountId(any());
    }

    // ---------- update(): duplicate name ----------

    @Test
    void update_withNameTakenAtTheTargetLevel_throwsConflictAndDoesNotSave() {
        givenAccountLocked();
        givenLocationExists(ROOM_ID, room);
        when(locationRepository.existsByAccountIdAndParentIdAndNameAndIdNot(ACCOUNT_ID, null, "Warehouse", room.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(ROOM_ID, request("Warehouse", null), ACCOUNT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Location with name 'Warehouse' already exists at this level");

        verify(locationRepository, never()).save(any());
    }

    // ---------- update(): account lock ----------

    @Test
    void update_whenAccountDoesNotExist_throwsNotFoundBeforeTouchingLocations() {
        when(accountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ROOM_ID, request("Room A", null), ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found");

        verifyNoInteractions(locationRepository);
    }

    // ---------- create() ----------

    @Test
    void create_underAParent_savesWithAccountAndParent() {
        givenAccountLocked();
        givenLocationExists(ROOM_ID, room);
        givenSaveReturnsTheSameLocation();

        service.create(request("Rack 2", ROOM_ID), ACCOUNT_ID);

        Location saved = capturedSavedLocation();
        assertThat(saved.getAccount()).isSameAs(account);
        assertThat(saved.getParent()).isSameAs(room);
        assertThat(saved.getName()).isEqualTo("Rack 2");
    }

    @Test
    void create_withNameTakenAtTheSameLevel_throwsConflictAndDoesNotSave() {
        givenAccountLocked();
        givenLocationExists(ROOM_ID, room);
        when(locationRepository.existsByAccountIdAndParentIdAndName(ACCOUNT_ID, room.getId(), "Rack 1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request("Rack 1", ROOM_ID), ACCOUNT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Location with name 'Rack 1' already exists at this level");

        verify(locationRepository, never()).save(any());
    }

    // ---------- delete() ----------

    @Test
    void delete_aLeafWithoutDevices_deletesIt() {
        givenAccountLocked();
        givenLocationExists(RACK_ID, rack);

        service.delete(RACK_ID, ACCOUNT_ID);

        verify(locationRepository).delete(rack);
    }

    @Test
    void delete_withChildLocations_throwsConflictAndDoesNotDelete() {
        givenAccountLocked();
        givenLocationExists(ROOM_ID, room);
        when(locationRepository.existsByParentId(room.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(ROOM_ID, ACCOUNT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Location has child locations; move or delete them first");

        verify(locationRepository, never()).delete(any());
    }

    @Test
    void delete_withAssignedDevices_throwsConflictAndDoesNotDelete() {
        givenAccountLocked();
        givenLocationExists(RACK_ID, rack);
        when(deviceRepository.existsByLocationId(rack.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(RACK_ID, ACCOUNT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Location has devices assigned; unassign them first");

        verify(locationRepository, never()).delete(any());
    }

    // ---------- helpers ----------

    private Location location(Long id, String name, Location parent) {
        Location location = new Location();
        location.setId(id);
        location.setName(name);
        location.setParent(parent);
        return location;
    }

    private LocationRequest request(String name, UUID parentPublicId) {
        LocationRequest request = new LocationRequest();
        request.setName(name);
        request.setParentPublicId(parentPublicId);
        return request;
    }

    private void givenAccountLocked() {
        when(accountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(account));
    }

    private void givenLocationExists(UUID publicId, Location location) {
        when(locationRepository.findByPublicIdAndAccountId(publicId, ACCOUNT_ID)).thenReturn(Optional.of(location));
    }

    private void givenTheAccountTree() {
        when(locationRepository.findByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(building, floor, room, rack, warehouse));
    }

    private void givenSaveReturnsTheSameLocation() {
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Location capturedSavedLocation() {
        ArgumentCaptor<Location> saved = ArgumentCaptor.forClass(Location.class);
        verify(locationRepository).save(saved.capture());
        return saved.getValue();
    }
}