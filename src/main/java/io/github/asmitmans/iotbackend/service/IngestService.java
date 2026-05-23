package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.ingest.IngestRequest;
import io.github.asmitmans.iotbackend.dto.ingest.IngestResponse;
import io.github.asmitmans.iotbackend.entity.Device;
import io.github.asmitmans.iotbackend.entity.DeviceConfig;
import io.github.asmitmans.iotbackend.entity.Measurement;
import io.github.asmitmans.iotbackend.repository.DeviceConfigRepository;
import io.github.asmitmans.iotbackend.repository.MeasurementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IngestService {

    private final MeasurementRepository measurementRepository;
    private final DeviceConfigRepository deviceConfigRepository;

    public IngestService(MeasurementRepository measurementRepository,
                         DeviceConfigRepository deviceConfigRepository) {
        this.measurementRepository = measurementRepository;
        this.deviceConfigRepository = deviceConfigRepository;
    }

    @Transactional
    public IngestResponse ingest(IngestRequest request, Device device) {

        Measurement measurement = new Measurement();
        measurement.setDevice(device);
        measurement.setCompanyId(device.getCompany().getId().longValue());
        measurement.setRecordedAt(request.getRecordedAt());
        measurement.setPayload(request.getPayload());
        measurement.setSequenceNumber(request.getSequenceNumber());
        measurement.setConfigChecksum(request.getConfigChecksum());

        deviceConfigRepository.findCurrentVersionByDeviceId(device.getId())
                              .ifPresent(measurement::setConfigVersion);

        measurementRepository.save(measurement);

        IngestResponse response = new IngestResponse();
        response.setStatus("OK");

        String storedHash = device.getCurrentConfigHash();
        String incomingHash = request.getConfigChecksum();

        if (incomingHash == null || storedHash == null) {
            response.setConfigAction("REPORT");
            return response;
        }

        if (!incomingHash.equals(storedHash)) {
            List<DeviceConfig> configs = deviceConfigRepository
                    .findByDeviceId(device.getId());

            boolean hasPending = configs.stream()
                                        .anyMatch(c -> c.getStatus().equals("PENDING"));

            if (hasPending) {
                Map<String, String> configMap = configs.stream()
                                                       .filter(c -> c.getDesiredValue() != null)
                                                       .collect(Collectors.toMap(
                                                               DeviceConfig::getKey,
                                                               DeviceConfig::getDesiredValue));
                response.setConfigAction("APPLY");
                response.setConfig(configMap);
            } else {
                response.setConfigAction("REPORT");
            }
            return response;
        }

        return response;
    }
}