package ua.nure.kryvko.hikeway.service;

import java.time.Instant;

public interface PoiPhotoStorage {
    PresignedUpload createUpload(String objectKey, String contentType, long sizeBytes);

    StoredObject inspect(String objectKey);

    void delete(String objectKey);

    String publicUrl(String objectKey);

    record PresignedUpload(String url, Instant expiresAt) {
    }

    record StoredObject(String contentType, long sizeBytes) {
    }
}
