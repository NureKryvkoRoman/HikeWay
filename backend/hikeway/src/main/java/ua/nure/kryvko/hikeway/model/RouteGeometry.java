package ua.nure.kryvko.hikeway.model;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.LineString;

@Entity
@Data
public class RouteGeometry {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    private Route route;

    @Column(columnDefinition = "geometry(LineString, 4326)")
    private LineString line;
}