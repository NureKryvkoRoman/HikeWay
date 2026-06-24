package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_route_poi", columnNames = {"route_id", "poi_id"}),
                @UniqueConstraint(name = "uk_route_poi_position", columnNames = {"route_id", "position"})
        },
        indexes = @Index(name = "idx_route_poi_route", columnList = "route_id,position")
)
@Data
public class RoutePoi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "poi_id", nullable = false)
    private PointOfInterest poi;

    @Column(nullable = false)
    private int position;
}
