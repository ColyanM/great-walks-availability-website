package com.example.greatwalkalerts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "itineraries")
public class Itinerary {

    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "walk_id", nullable = false)
    private Walk walk;

    @Column(nullable = false, length = 100)
    private String name;

    protected Itinerary() {
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
}