package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(indexes = {
        @Index(name = "idx_poi_owner", columnList = "ownerId"),
        @Index(name = "idx_poi_deleted", columnList = "deleted")
})
@Data
public class PointOfInterest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false)
    private String ownerDisplayName;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;
}
