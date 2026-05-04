package com.example.model;

import jakarta.persistence.*;

@Entity
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    public Ingredient() {}

    public Ingredient(String name) { this.name = normalize(name); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = normalize(name); }

    private static String normalize(String name) {
        if (name == null) return null;
        return name.trim().toLowerCase();
    }
}
