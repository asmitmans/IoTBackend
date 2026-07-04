package io.github.asmitmans.iotbackend.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "device_config")
public class DeviceConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "config_version", nullable = false)
    private Long configVersion;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "desired_value", length = 255)
    private String desiredValue;

    @Column(name = "reported_value", length = 255)
    private String reportedValue;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    public DeviceConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Long getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(Long configVersion) {
        this.configVersion = configVersion;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDesiredValue() {
        return desiredValue;
    }

    public void setDesiredValue(String desiredValue) {
        this.desiredValue = desiredValue;
    }

    public String getReportedValue() {
        return reportedValue;
    }

    public void setReportedValue(String reportedValue) {
        this.reportedValue = reportedValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DeviceConfig that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
