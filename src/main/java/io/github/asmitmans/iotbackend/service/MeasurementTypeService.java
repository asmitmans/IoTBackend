package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.measurementtype.MeasurementTypeRequest;
import io.github.asmitmans.iotbackend.dto.measurementtype.MeasurementTypeResponse;
import io.github.asmitmans.iotbackend.entity.MeasurementType;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.MeasurementTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeasurementTypeService {

    private final MeasurementTypeRepository measurementTypeRepository;

    public MeasurementTypeService(MeasurementTypeRepository measurementTypeRepository) {
        this.measurementTypeRepository = measurementTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<MeasurementTypeResponse> getAll() {
        return measurementTypeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeasurementTypeResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public MeasurementTypeResponse create(MeasurementTypeRequest request) {
        measurementTypeRepository.findByName(request.getName())
                .ifPresent(m -> {
                    throw new ConflictException("MeasurementType with name '" + request.getName() + "' already exists");
                });

        MeasurementType entity = new MeasurementType();
        applyRequest(entity, request);
        return toResponse(measurementTypeRepository.save(entity));
    }

    @Transactional
    public MeasurementTypeResponse update(Long id, MeasurementTypeRequest request) {
        MeasurementType entity = findOrThrow(id);
        applyRequest(entity, request);
        return toResponse(measurementTypeRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        MeasurementType entity = findOrThrow(id);
        measurementTypeRepository.delete(entity);
    }

    private void applyRequest(MeasurementType entity, MeasurementTypeRequest request) {
        entity.setName(request.getName());
        entity.setSiUnit(request.getSiUnit());
        entity.setCategory(request.getCategory());
        entity.setDataType(request.getDataType());
    }

    private MeasurementType findOrThrow(Long id) {
        return measurementTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MeasurementType not found"));
    }

    private MeasurementTypeResponse toResponse(MeasurementType entity) {
        MeasurementTypeResponse r = new MeasurementTypeResponse();
        r.setId(entity.getId());
        r.setName(entity.getName());
        r.setSiUnit(entity.getSiUnit());
        r.setCategory(entity.getCategory());
        r.setDataType(entity.getDataType());
        return r;
    }

}
