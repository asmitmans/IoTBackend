package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.commandtype.CommandTypeResponse;
import io.github.asmitmans.iotbackend.dto.devicemodel.DeviceModelDetailResponse;
import io.github.asmitmans.iotbackend.dto.devicemodel.DeviceModelListResponse;
import io.github.asmitmans.iotbackend.dto.devicemodel.DeviceModelRequest;
import io.github.asmitmans.iotbackend.dto.measurementtype.MeasurementTypeResponse;
import io.github.asmitmans.iotbackend.entity.CommandType;
import io.github.asmitmans.iotbackend.entity.DeviceModel;
import io.github.asmitmans.iotbackend.entity.MeasurementType;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.CommandTypeRepository;
import io.github.asmitmans.iotbackend.repository.DeviceModelRepository;
import io.github.asmitmans.iotbackend.repository.MeasurementTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeviceModelService {

    private final DeviceModelRepository deviceModelRepository;
    private final MeasurementTypeRepository measurementTypeRepository;
    private final CommandTypeRepository commandTypeRepository;

    public DeviceModelService(DeviceModelRepository deviceModelRepository, MeasurementTypeRepository measurementTypeRepository, CommandTypeRepository commandTypeRepository) {
        this.deviceModelRepository = deviceModelRepository;
        this.measurementTypeRepository = measurementTypeRepository;
        this.commandTypeRepository = commandTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<DeviceModelListResponse> getAll() {
        return deviceModelRepository.findAll()
                .stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeviceModelDetailResponse getById(Long id) {
        return toDetailResponse(findOrThrow(id));
    }

    @Transactional
    public DeviceModelDetailResponse create(DeviceModelRequest request) {
        deviceModelRepository.findByName(request.getName())
                .ifPresent(m -> {
                    throw new ConflictException("DeviceModel with name '" + request.getName() + "' already exists");
                });

        DeviceModel entity = new DeviceModel();
        entity.setName(request.getName());
        applyAssociations(entity, request);

        return toDetailResponse(deviceModelRepository.save(entity));
    }

    @Transactional
    public DeviceModelDetailResponse update(Long id, DeviceModelRequest request) {
        DeviceModel entity = findOrThrow(id);
        entity.setName(request.getName());
        applyAssociations(entity, request);

        return toDetailResponse(deviceModelRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        DeviceModel entity = findOrThrow(id);
        deviceModelRepository.delete(entity);
    }

    private void applyAssociations(DeviceModel entity, DeviceModelRequest request) {
        if (request.getMeasurementTypeIds() != null) {
            Set<MeasurementType> measurementTypes = new HashSet<>(
                    measurementTypeRepository.findByIdIn(request.getMeasurementTypeIds()));
            entity.setMeasurementTypes(measurementTypes);
        }

        if (request.getCommandTypeIds() != null) {
            Set<CommandType> commandTypes = new HashSet<>(
                    commandTypeRepository.findByIdIn(request.getCommandTypeIds()));
            entity.setCommandTypes(commandTypes);
        }
    }

    private DeviceModel findOrThrow(Long id) {
        return deviceModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceModel not found"));
    }

    private DeviceModelListResponse toListResponse(DeviceModel entity) {
        DeviceModelListResponse r = new DeviceModelListResponse();
        r.setId(entity.getId());
        r.setName(entity.getName());
        return r;
    }

    private DeviceModelDetailResponse toDetailResponse(DeviceModel entity) {
        DeviceModelDetailResponse r = new DeviceModelDetailResponse();
        r.setId(entity.getId());
        r.setName(entity.getName());

        r.setMeasurementTypes(entity.getMeasurementTypes().stream()
                .map(mt -> {
                    MeasurementTypeResponse mtr = new MeasurementTypeResponse();
                    mtr.setId(mt.getId());
                    mtr.setName(mt.getName());
                    mtr.setSiUnit(mt.getSiUnit());
                    mtr.setCategory(mt.getCategory());
                    mtr.setDataType(mt.getDataType());
                    return mtr;
                })
                .collect(Collectors.toSet()));

        r.setCommandTypes(entity.getCommandTypes().stream()
                .map(ct -> {
                    CommandTypeResponse ctr = new CommandTypeResponse();
                    ctr.setId(ct.getId());
                    ctr.setName(ct.getName());
                    ctr.setDescription(ct.getDescription());
                    return ctr;
                })
                .collect(Collectors.toSet()));

        return r;
    }

}