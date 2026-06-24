package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(name = "uk_poi_rating_user", columnNames = {"poi_id", "userId"}),
        indexes = @Index(name = "idx_poi_rating_poi", columnList = "poi_id")
)
@Data
public class PoiRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "poi_id", nullable = false)
    private PointOfInterest poi;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private Instant updatedAt;
}
