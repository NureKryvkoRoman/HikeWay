package ua.nure.kryvko.hikeway.model.request;

public final class PoiRequests {
    private PoiRequests() {
    }

    public record Create(
            String name,
            String description,
            Double longitude,
            Double latitude
    ) {
    }

    public record Update(
            String name,
            String description,
            Double longitude,
            Double latitude
    ) {
    }

    public record Rating(Integer score) {
    }

    public record Comment(String text) {
    }

    public record PhotoUpload(String contentType, Long sizeBytes) {
    }

    public record PhotoFinalize(Long photoId, String caption) {
    }

    public record PhotoUpdate(String caption) {
    }
}
