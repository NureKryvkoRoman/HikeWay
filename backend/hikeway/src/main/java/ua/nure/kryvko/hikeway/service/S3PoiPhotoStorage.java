package ua.nure.kryvko.hikeway.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import ua.nure.kryvko.hikeway.config.PoiStorageProperties;
import ua.nure.kryvko.hikeway.exception.InvalidPoiDataException;

import java.time.Duration;
import java.time.Instant;

@Service
public class S3PoiPhotoStorage implements PoiPhotoStorage {
    private static final Duration DEFAULT_EXPIRY = Duration.ofMinutes(15);

    private final S3Client client;
    private final S3Presigner presigner;
    private final PoiStorageProperties properties;

    public S3PoiPhotoStorage(
            S3Client client,
            S3Presigner presigner,
            PoiStorageProperties properties
    ) {
        this.client = client;
        this.presigner = presigner;
        this.properties = properties;
    }

    @Override
    public PresignedUpload createUpload(String objectKey, String contentType, long sizeBytes) {
        Duration expiry = properties.uploadExpiry() == null ? DEFAULT_EXPIRY : properties.uploadExpiry();
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(sizeBytes)
                .build();
        var request = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(objectRequest)
                .build();
        return new PresignedUpload(
                presigner.presignPutObject(request).url().toString(),
                Instant.now().plus(expiry)
        );
    }

    @Override
    public StoredObject inspect(String objectKey) {
        try {
            var response = client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());
            return new StoredObject(response.contentType(), response.contentLength());
        } catch (NoSuchKeyException exception) {
            throw new InvalidPoiDataException("INVALID_UPLOAD", "Uploaded object was not found");
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new InvalidPoiDataException("INVALID_UPLOAD", "Uploaded object was not found");
            }
            throw exception;
        }
    }

    @Override
    public void delete(String objectKey) {
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build());
    }

    @Override
    public String publicUrl(String objectKey) {
        String base = properties.publicBaseUrl().replaceAll("/+$", "");
        return base + "/" + objectKey;
    }
}
