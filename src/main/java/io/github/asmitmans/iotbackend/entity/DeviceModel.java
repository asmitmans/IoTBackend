package io.github.asmitmans.iotbackend.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "device_model")
public class DeviceModel extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "device_model_measurement",  // ← correcto
            joinColumns = @JoinColumn(name = "device_model_id"),
            inverseJoinColumns = @JoinColumn(name = "measurement_type_id")
    )
    private Set<MeasurementType> measurementTypes = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "device_model_command",
            joinColumns = @JoinColumn(name = "device_model_id"),
            inverseJoinColumns = @JoinColumn(name = "command_type_id")
    )
    private Set<CommandType> commandTypes = new HashSet<>();

    public DeviceModel() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<MeasurementType> getMeasurementTypes() {
        return measurementTypes;
    }

    public void setMeasurementTypes(Set<MeasurementType> measurementTypes) {
        this.measurementTypes = measurementTypes;
    }

    public Set<CommandType> getCommandTypes() {
        return commandTypes;
    }

    public void setCommandTypes(Set<CommandType> commandTypes) {
        this.commandTypes = commandTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceModel)) return false;
        DeviceModel that = (DeviceModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
