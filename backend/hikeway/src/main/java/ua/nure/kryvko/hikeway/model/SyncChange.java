package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(indexes = @Index(columnList = "ownerId,id"))
@Data
public class SyncChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    private SyncResourceType resourceType;

    @Column(nullable = false)
    private UUID clientId;

    private long resourceVersion;
    private boolean deleted;
    private Instant createdAt;
}
