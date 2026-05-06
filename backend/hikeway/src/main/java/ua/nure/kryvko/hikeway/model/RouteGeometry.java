package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.LineString;

@Entity
public class RouteGeometry {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    private Route route;

    @Column(columnDefinition = "geometry(LineString, 4326)")
    private LineString line;
}