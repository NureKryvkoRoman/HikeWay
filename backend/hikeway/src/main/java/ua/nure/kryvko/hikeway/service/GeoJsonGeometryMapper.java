package ua.nure.kryvko.hikeway.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Component;
import ua.nure.kryvko.hikeway.exception.InvalidRouteGeometryException;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;

import java.util.Arrays;
import java.util.List;

@Component
public class GeoJsonGeometryMapper {
    private static final String LINE_STRING_TYPE = "LineString";
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public LineString toLineString(GeoJsonLineString geometry, boolean allowEmpty) {
        if (geometry == null && allowEmpty) {
            return null;
        }
        if (geometry == null || !LINE_STRING_TYPE.equals(geometry.type())) {
            throw new InvalidRouteGeometryException("Geometry must be a GeoJSON LineString");
        }
        int minimumPoints = allowEmpty ? 0 : 2;
        if (geometry.coordinates() == null ||
                (!geometry.coordinates().isEmpty() && geometry.coordinates().size() < 2) ||
                geometry.coordinates().size() < minimumPoints) {
            throw new InvalidRouteGeometryException("LineString must contain at least two coordinates");
        }
        Coordinate[] coordinates = geometry.coordinates().stream()
                .map(this::toCoordinate)
                .toArray(Coordinate[]::new);
        LineString line = geometryFactory.createLineString(coordinates);
        line.setSRID(4326);
        return line;
    }

    public GeoJsonLineString toGeoJson(LineString line) {
        if (line == null) {
            return new GeoJsonLineString(LINE_STRING_TYPE, List.of());
        }
        return new GeoJsonLineString(
                LINE_STRING_TYPE,
                Arrays.stream(line.getCoordinates())
                        .map(coordinate -> List.of(coordinate.getX(), coordinate.getY()))
                        .toList()
        );
    }

    private Coordinate toCoordinate(List<Double> coordinate) {
        if (coordinate == null || coordinate.size() < 2 ||
                coordinate.get(0) == null || coordinate.get(1) == null) {
            throw new InvalidRouteGeometryException("Coordinates require longitude and latitude");
        }
        double longitude = coordinate.get(0);
        double latitude = coordinate.get(1);
        if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new InvalidRouteGeometryException("Coordinate is outside valid longitude/latitude bounds");
        }
        return new Coordinate(longitude, latitude);
    }
}
