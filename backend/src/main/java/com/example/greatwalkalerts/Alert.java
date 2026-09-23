package com.example.greatwalkalerts;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "walk_id", nullable = false)
    private int walkId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    protected Alert() {
    }

    public Alert(int walkId, LocalDate startDate, int partySize) {
        this.walkId = walkId;
        this.startDate = startDate;
        this.partySize = partySize;
    }

    public Integer getId() {
        return id;
    }

    public int getWalkId() {
    return walkId;
}

public LocalDate getStartDate() {
    return startDate;
}

public int getPartySize() {
    return partySize;
}
}