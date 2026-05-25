package io.github.asmitmans.iotbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "device")
public class Device extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_model_id", nullable = false)
    private DeviceModel deviceModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(nullable = false, length = 100, unique = true)
    private String serialNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String apiKeyHash;

    @Column(length = 8)
    private String apiKeyPrefix;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claim_expires_at")
    private Instant claimExpiresAt;

    @Column(name = "config_pending", nullable = false)
    private boolean configPending = false;

    @Column(name = "command_pending", nullable = false)
    private boolean commandPending = false;

    @Column(nullable = false, length = 20)
    private String status;

    public Device() {}

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Device device)) return false;
        return Objects.equals(id, device.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public DeviceModel getDeviceModel() { return deviceModel; }
    public void setDeviceModel(DeviceModel deviceModel) { this.deviceModel = deviceModel; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; }

    public String getApiKeyPrefix() { return apiKeyPrefix; }
    public void setApiKeyPrefix(String apiKeyPrefix) { this.apiKeyPrefix = apiKeyPrefix; }

    public Instant getClaimedAt() { return claimedAt; }
    public void setClaimedAt(Instant claimedAt) { this.claimedAt = claimedAt; }

    public Instant getClaimExpiresAt() { return claimExpiresAt; }
    public void setClaimExpiresAt(Instant claimExpiresAt) { this.claimExpiresAt = claimExpiresAt; }

    public boolean isConfigPending() { return configPending; }
    public void setConfigPending(boolean configPending) { this.configPending = configPending; }

    public boolean isCommandPending() { return commandPending; }
    public void setCommandPending(boolean commandPending) { this.commandPending = commandPending; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}