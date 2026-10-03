package com.example.greatwalkalerts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "facilities")
public class Facility {

    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "walk_id", nullable = false)
    private Walk walk;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", nullable = false, length = 20)
    private FacilityType facilityType;

    @Column(name = "doc_facility_id")
    private Integer docFacilityId;

    public Integer getDocFacilityId() {
        return docFacilityId;
    }

    protected Facility() {
    }

    public Integer getId() {
        return id;
    }

    public Walk getWalk() {
        return walk;
    }

    public String getName() {
        return name;
    }

    public FacilityType getFacilityType() {
        return facilityType;
    }
}