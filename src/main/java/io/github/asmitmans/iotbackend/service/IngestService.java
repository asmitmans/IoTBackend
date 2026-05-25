package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.ingest.IngestRequest;
import io.github.asmitmans.iotbackend.dto.ingest.IngestResponse;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.Measurement;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import io.github.asmitmans.iotbackend.repository.MeasurementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class IngestService {

    private final MeasurementRepository measurementRepository;

    public IngestService(MeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    @Transactional
    public IngestResponse ingest(IngestRequest request, Device device) {

        Measurement measurement = new Measurement();
        measurement.setDevice(device);
        measurement.setCompanyId(device.getCompany().getId().longValue());
        measurement.setRecordedAt(request.getRecordedAt());
        measurement.setPayload(request.getPayload());
        measurement.setSequenceNumber(request.getSequenceNumber());
        measurementRepository.save(measurement);

        IngestResponse respose = new IngestResponse();
        respose.setStatus("OK");
        respose.setConfigPending(device.isConfigPending());
        respose.setCommandPending(device.isCommandPending());

        return respose;
    }
}
