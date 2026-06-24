package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"createdBy", "clientId"}),
        indexes = @Index(columnList = "createdBy,clientId")
)
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

    @Column(nullable = false, columnDefinition = "uuid default gen_random_uuid()")
    private UUID clientId = UUID.randomUUID();

    @Column(nullable = false, columnDefinition = "bigint default 1")
    private long syncVersion = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'MIXED'")
    private Terrain terrain = Terrain.MIXED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'PRIVATE'")
    private RouteVisibility visibility = RouteVisibility.PRIVATE;

    private Instant updatedAt;

    private Instant publishedAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted;
}
