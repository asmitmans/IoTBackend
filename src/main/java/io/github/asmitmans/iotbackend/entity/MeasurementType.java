package io.github.asmitmans.iotbackend.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "measurement_type")
public class MeasurementType extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "si_unit", length = 20)
    private String siUnit;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    public MeasurementType() {
    }

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

    public String getSiUnit() {
        return siUnit;
    }

    public void setSiUnit(String siUnit) {
        this.siUnit = siUnit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MeasurementType that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
