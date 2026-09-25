package com.example.greatwalkalerts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "walks")
public class Walk {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    protected Walk() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}