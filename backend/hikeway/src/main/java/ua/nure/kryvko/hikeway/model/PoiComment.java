package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "idx_poi_comment_poi_created", columnList = "poi_id,createdAt"))
@Data
public class PoiComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "poi_id", nullable = false)
    private PointOfInterest poi;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false)
    private String authorDisplayName;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;
}
