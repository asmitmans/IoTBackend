package io.github.asmitmans.iotbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.asmitmans.iotbackend.dto.ingest.IngestRequest;
import io.github.asmitmans.iotbackend.dto.ingest.IngestResponse;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.Measurement;
import io.github.asmitmans.iotbackend.repository.MeasurementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestService {

    private final MeasurementRepository measurementRepository;
    private final ObjectMapper objectMapper;

    public IngestService(MeasurementRepository measurementRepository, ObjectMapper objectMapper) {
        this.measurementRepository = measurementRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IngestResponse ingest(IngestRequest request, Device device) {

        try {
            objectMapper.readTree(request.getPayload());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON payload: " + e.getOriginalMessage());
        }

        Measurement measurement = new Measurement();
        measurement.setDevice(device);
        measurement.setAccountId(device.getAccount().getId().longValue());
        measurement.setRecordedAt(request.getRecordedAt());
        measurement.setPayload(request.getPayload());
        measurement.setSequenceNumber(request.getSequenceNumber());
        measurementRepository.save(measurement);

        IngestResponse response = new IngestResponse();
        response.setStatus("OK");
        response.setConfigPending(device.isConfigPending());
        response.setCommandPending(device.isCommandPending());

        return response;
    }
}