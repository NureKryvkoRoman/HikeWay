package ua.nure.kryvko.hikeway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "hikeway.poi.storage")
public record PoiStorageProperties(
        URI endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket,
        String publicBaseUrl,
        Duration uploadExpiry
) {
}
