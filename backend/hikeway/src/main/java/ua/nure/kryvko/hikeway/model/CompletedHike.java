package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.LineString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"ownerId", "clientId"}),
        indexes = @Index(columnList = "ownerId,clientId")
)
@Data
public class CompletedHike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false)
    private UUID clientId;

    @Column(nullable = false)
    private long syncVersion;

    private UUID routeClientId;
    private Long routeServerId;

    @Column(nullable = false)
    private String routeName;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant finishedAt;

    private long activeDurationMillis;
    private long wallClockDurationMillis;
    private double totalDistanceKm;

    @Column(columnDefinition = "geometry(LineString, 4326)")
    private LineString path;

    private Instant createdAt;
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;
}
