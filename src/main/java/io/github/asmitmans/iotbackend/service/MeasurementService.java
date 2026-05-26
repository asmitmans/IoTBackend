package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.measurement.MeasurementResponse;
import io.github.asmitmans.iotbackend.entity.Measurement;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import io.github.asmitmans.iotbackend.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final DeviceRepository deviceRepository;

    public MeasurementService(MeasurementRepository measurementRepository, DeviceRepository deviceRepository) {
        this.measurementRepository = measurementRepository;
        this.deviceRepository = deviceRepository;
    }

    public List<MeasurementResponse> getLatest(List<Long> deviceIds,
                                               Long companyId) {
        List<Long> ids = resolveDeviceIds(deviceIds, companyId);
        return ids.stream()
                  .map(id -> measurementRepository.findLatestByDeviceId(id))
                .filter(opt -> opt.isPresent())
                .map(opt -> toResponse(opt.get()))
                .collect(Collectors.toList());
    }

    private List<Long> resolveDeviceIds(List<Long> deviceIds, Long companyId) {
        if (deviceIds != null && !deviceIds.isEmpty()) {
            return deviceIds;
        }
        return deviceRepository.findByCompanyId(companyId)
                               .stream()
                               .map(d -> d.getId())
                               .collect(Collectors.toList());
    }

    private MeasurementResponse toResponse(Measurement m) {
        MeasurementResponse r = new MeasurementResponse();
        r.setId(m.getId());
        r.setDeviceId(m.getDevice().getId());
        r.setDeviceName(m.getDevice().getName());
        r.setSerialNumber(m.getDevice().getSerialNumber());
        r.setRecordedAt(m.getRecordedAt());
        r.setReceivedAt(m.getReceivedAt());
        r.setPayload(m.getPayload());
        return r;
    }
}