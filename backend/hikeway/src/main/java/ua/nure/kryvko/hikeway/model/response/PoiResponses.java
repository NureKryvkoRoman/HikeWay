package ua.nure.kryvko.hikeway.model.response;

import java.time.Instant;
import java.util.List;

public final class PoiResponses {
    private PoiResponses() {
    }

    public record Summary(
            long id,
            String name,
            String description,
            double longitude,
            double latitude,
            String ownerId,
            String ownerDisplayName,
            boolean ownedByCurrentUser,
            double averageRating,
            long ratingCount,
            Integer userRating,
            List<Photo> photos
    ) {
    }

    public record NearbySummary(
            long id,
            String name,
            String description,
            double longitude,
            double latitude,
            String ownerDisplayName,
            boolean ownedByCurrentUser,
            double distanceMeters
    ) {
    }

    public record Detail(
            long id,
            String name,
            String description,
            double longitude,
            double latitude,
            String ownerId,
            String ownerDisplayName,
            boolean ownedByCurrentUser,
            double averageRating,
            long ratingCount,
            Integer userRating,
            List<Photo> photos,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record Rating(double averageRating, long ratingCount, Integer userRating) {
    }

    public record Comment(
            long id,
            String authorId,
            String authorDisplayName,
            boolean ownedByCurrentUser,
            String text,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record Photo(
            long id,
            String contributorId,
            String contributorDisplayName,
            boolean ownedByCurrentUser,
            String url,
            String contentType,
            long sizeBytes,
            String caption,
            Instant createdAt
    ) {
    }

    public record Upload(
            long photoId,
            String objectKey,
            String uploadUrl,
            Instant expiresAt,
            String contentType,
            long sizeBytes
    ) {
    }
}
