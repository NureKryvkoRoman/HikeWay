package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private double distanceKm;

    private int estimatedTimeMinutes;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private int elevationGain;

    private Instant createdAt;

    /**
     * Keycloak ID
     */
    private String createdBy;
}