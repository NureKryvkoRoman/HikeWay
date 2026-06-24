package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(name = "uk_poi_photo_object_key", columnNames = "objectKey"),
        indexes = @Index(name = "idx_poi_photo_poi_status", columnList = "poi_id,status")
)
@Data
public class PoiPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "poi_id", nullable = false)
    private PointOfInterest poi;

    @Column(nullable = false)
    private String contributorId;

    @Column(nullable = false)
    private String contributorDisplayName;

    @Column(nullable = false, length = 700)
    private String objectKey;

    @Column(length = 1000)
    private String publicUrl;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(length = 500)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PoiPhotoStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant readyAt;
}
