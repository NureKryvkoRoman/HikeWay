package ua.nure.kryvko.hikeway.repository;

public interface NearbyPoiProjection {
    Long getId();

    String getName();

    String getDescription();

    Double getLongitude();

    Double getLatitude();

    String getOwnerId();

    String getOwnerDisplayName();

    Double getDistanceMeters();
}
