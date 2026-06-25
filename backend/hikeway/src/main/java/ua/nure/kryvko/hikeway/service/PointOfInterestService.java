package ua.nure.kryvko.hikeway.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.nure.kryvko.hikeway.exception.ForbiddenOperationException;
import ua.nure.kryvko.hikeway.exception.InvalidPoiDataException;
import ua.nure.kryvko.hikeway.exception.PoiContentNotFoundException;
import ua.nure.kryvko.hikeway.exception.PoiNotFoundException;
import ua.nure.kryvko.hikeway.model.*;
import ua.nure.kryvko.hikeway.model.request.PoiRequests;
import ua.nure.kryvko.hikeway.model.response.PageResponse;
import ua.nure.kryvko.hikeway.model.response.PoiResponses;
import ua.nure.kryvko.hikeway.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PointOfInterestService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final double MAX_NEARBY_RADIUS_METERS = 100_000;
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;
    private static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final PointOfInterestRepository poiRepository;
    private final PoiRatingRepository ratingRepository;
    private final PoiCommentRepository commentRepository;
    private final PoiPhotoRepository photoRepository;
    private final RoutePoiRepository routePoiRepository;
    private final CurrentUserService currentUser;
    private final PoiPhotoStorage photoStorage;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public PointOfInterestService(
            PointOfInterestRepository poiRepository,
            PoiRatingRepository ratingRepository,
            PoiCommentRepository commentRepository,
            PoiPhotoRepository photoRepository,
            RoutePoiRepository routePoiRepository,
            CurrentUserService currentUser,
            PoiPhotoStorage photoStorage
    ) {
        this.poiRepository = poiRepository;
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.photoRepository = photoRepository;
        this.routePoiRepository = routePoiRepository;
        this.currentUser = currentUser;
        this.photoStorage = photoStorage;
    }

    @Transactional(readOnly = true)
    public PageResponse<PoiResponses.Summary> list(
            int page,
            int size,
            Double minLongitude,
            Double minLatitude,
            Double maxLongitude,
            Double maxLatitude
    ) {
        PageRequest pageable = pageRequest(page, size, Sort.by("updatedAt").descending());
        boolean hasAnyBounds = minLongitude != null || minLatitude != null ||
                maxLongitude != null || maxLatitude != null;
        if (hasAnyBounds) {
            requireLongitude(minLongitude);
            requireLatitude(minLatitude);
            requireLongitude(maxLongitude);
            requireLatitude(maxLatitude);
            if (minLongitude > maxLongitude || minLatitude > maxLatitude) {
                throw invalidLocation("Minimum bounds must not exceed maximum bounds");
            }
        }
        var pois = hasAnyBounds
                ? poiRepository.findWithinBounds(
                        minLongitude,
                        minLatitude,
                        maxLongitude,
                        maxLatitude,
                        pageable
                )
                : poiRepository.findByDeletedFalse(pageable);
        return PageResponse.from(pois, pois.stream().map(this::toSummary).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<PoiResponses.NearbySummary> nearby(
            Double longitude,
            Double latitude,
            Double radiusMeters,
            int page,
            int size
    ) {
        double validLongitude = requireLongitude(longitude);
        double validLatitude = requireLatitude(latitude);
        double validRadius = requireNearbyRadius(radiusMeters);
        var pois = poiRepository.findNearby(
                validLongitude,
                validLatitude,
                validRadius,
                pageRequest(page, size, Sort.unsorted())
        );
        String userId = currentUser.id();
        return PageResponse.from(pois, pois.stream()
                .map(poi -> new PoiResponses.NearbySummary(
                        poi.getId(),
                        poi.getName(),
                        poi.getDescription(),
                        poi.getLongitude(),
                        poi.getLatitude(),
                        poi.getOwnerDisplayName(),
                        poi.getOwnerId().equals(userId),
                        poi.getDistanceMeters()
                ))
                .toList());
    }

    @Transactional(readOnly = true)
    public PoiResponses.Detail get(long id) {
        PointOfInterest poi = activePoi(id);
        RatingAggregate aggregate = aggregate(id);
        return new PoiResponses.Detail(
                poi.getId(),
                poi.getName(),
                poi.getDescription(),
                poi.getLocation().getX(),
                poi.getLocation().getY(),
                poi.getOwnerId(),
                poi.getOwnerDisplayName(),
                poi.getOwnerId().equals(currentUser.id()),
                aggregate.average(),
                aggregate.count(),
                aggregate.userScore(),
                photos(poi.getId()),
                poi.getCreatedAt(),
                poi.getUpdatedAt()
        );
    }

    @Transactional
    public PoiResponses.Detail create(PoiRequests.Create request) {
        String name = requiredText(request.name(), "Name", 120);
        String description = requiredText(request.description(), "Description", 4000);
        PointOfInterest poi = new PointOfInterest();
        poi.setOwnerId(currentUser.id());
        poi.setOwnerDisplayName(currentUser.displayName());
        poi.setName(name);
        poi.setDescription(description);
        poi.setLocation(point(request.longitude(), request.latitude()));
        Instant now = Instant.now();
        poi.setCreatedAt(now);
        poi.setUpdatedAt(now);
        poi = poiRepository.save(poi);
        return get(poi.getId());
    }

    @Transactional
    public PoiResponses.Detail update(long id, PoiRequests.Update request) {
        PointOfInterest poi = ownedPoi(id);
        if (request.name() != null) {
            poi.setName(requiredText(request.name(), "Name", 120));
        }
        if (request.description() != null) {
            poi.setDescription(requiredText(request.description(), "Description", 4000));
        }
        if (request.longitude() != null || request.latitude() != null) {
            poi.setLocation(point(request.longitude(), request.latitude()));
        }
        poi.setUpdatedAt(Instant.now());
        poiRepository.save(poi);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        PointOfInterest poi = ownedPoi(id);
        poi.setDeleted(true);
        poi.setUpdatedAt(Instant.now());
        commentRepository.findByPoiIdAndDeletedFalse(id)
                .forEach(comment -> comment.setDeleted(true));
        photoRepository.findByPoiId(id).stream()
                .filter(photo -> photo.getStatus() != PoiPhotoStatus.DELETED)
                .forEach(photo -> {
                    photoStorage.delete(photo.getObjectKey());
                    photo.setStatus(PoiPhotoStatus.DELETED);
                });
        ratingRepository.deleteByPoiId(id);
        routePoiRepository.deleteByPoiId(id);
        poiRepository.save(poi);
    }

    @Transactional
    public PoiResponses.Rating rate(long id, PoiRequests.Rating request) {
        PointOfInterest poi = activePoi(id);
        if (request.score() == null || request.score() < 1 || request.score() > 5) {
            throw new InvalidPoiDataException("INVALID_RATING", "Rating must be between 1 and 5");
        }
        PoiRating rating = ratingRepository.findByPoiIdAndUserId(id, currentUser.id())
                .orElseGet(PoiRating::new);
        rating.setPoi(poi);
        rating.setUserId(currentUser.id());
        rating.setScore(request.score());
        rating.setUpdatedAt(Instant.now());
        ratingRepository.save(rating);
        return ratingResponse(id);
    }

    @Transactional
    public PoiResponses.Rating removeRating(long id) {
        activePoi(id);
        ratingRepository.findByPoiIdAndUserId(id, currentUser.id()).ifPresent(ratingRepository::delete);
        return ratingResponse(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<PoiResponses.Comment> comments(long poiId, int page, int size) {
        activePoi(poiId);
        var comments = commentRepository.findByPoiIdAndDeletedFalse(
                poiId,
                pageRequest(page, size, Sort.by("createdAt").descending())
        );
        String userId = currentUser.id();
        return PageResponse.from(comments, comments.stream()
                .map(comment -> toComment(comment, userId))
                .toList());
    }

    @Transactional
    public PoiResponses.Comment addComment(long poiId, PoiRequests.Comment request) {
        PointOfInterest poi = activePoi(poiId);
        PoiComment comment = new PoiComment();
        comment.setPoi(poi);
        comment.setAuthorId(currentUser.id());
        comment.setAuthorDisplayName(currentUser.displayName());
        comment.setText(requiredText(request.text(), "Comment", 2000));
        Instant now = Instant.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comment = commentRepository.save(comment);
        return toComment(comment, currentUser.id());
    }

    @Transactional
    public PoiResponses.Comment updateComment(long poiId, long commentId, PoiRequests.Comment request) {
        activePoi(poiId);
        PoiComment comment = ownedComment(poiId, commentId);
        comment.setText(requiredText(request.text(), "Comment", 2000));
        comment.setUpdatedAt(Instant.now());
        return toComment(commentRepository.save(comment), currentUser.id());
    }

    @Transactional
    public void deleteComment(long poiId, long commentId) {
        activePoi(poiId);
        PoiComment comment = ownedComment(poiId, commentId);
        comment.setDeleted(true);
        comment.setUpdatedAt(Instant.now());
        commentRepository.save(comment);
    }

    @Transactional
    public PoiResponses.Upload createPhotoUpload(long poiId, PoiRequests.PhotoUpload request) {
        PointOfInterest poi = activePoi(poiId);
        String contentType = normalizePhotoType(request.contentType());
        long sizeBytes = requirePhotoSize(request.sizeBytes());
        String objectKey = "pois/" + poiId + "/" + UUID.randomUUID() + extension(contentType);

        PoiPhoto photo = new PoiPhoto();
        photo.setPoi(poi);
        photo.setContributorId(currentUser.id());
        photo.setContributorDisplayName(currentUser.displayName());
        photo.setObjectKey(objectKey);
        photo.setContentType(contentType);
        photo.setSizeBytes(sizeBytes);
        photo.setStatus(PoiPhotoStatus.PENDING);
        photo.setCreatedAt(Instant.now());
        photo = photoRepository.save(photo);

        var upload = photoStorage.createUpload(objectKey, contentType, sizeBytes);
        return new PoiResponses.Upload(
                photo.getId(),
                objectKey,
                upload.url(),
                upload.expiresAt(),
                contentType,
                sizeBytes
        );
    }

    @Transactional
    public PoiResponses.Photo finalizePhoto(long poiId, PoiRequests.PhotoFinalize request) {
        activePoi(poiId);
        if (request.photoId() == null) {
            throw invalidUpload("Photo ID is required");
        }
        PoiPhoto photo = photoRepository.findByIdAndPoiId(request.photoId(), poiId)
                .orElseThrow(() -> new PoiContentNotFoundException("Photo"));
        requireOwner(photo.getContributorId());
        if (photo.getStatus() != PoiPhotoStatus.PENDING) {
            throw invalidUpload("Photo upload is not pending");
        }
        var object = photoStorage.inspect(photo.getObjectKey());
        if (object.sizeBytes() != photo.getSizeBytes() ||
                !photo.getContentType().equalsIgnoreCase(object.contentType())) {
            photoStorage.delete(photo.getObjectKey());
            photo.setStatus(PoiPhotoStatus.DELETED);
            photoRepository.save(photo);
            throw invalidUpload("Uploaded photo metadata does not match the upload request");
        }
        photo.setCaption(optionalText(request.caption(), 500));
        photo.setPublicUrl(photoStorage.publicUrl(photo.getObjectKey()));
        photo.setStatus(PoiPhotoStatus.READY);
        photo.setReadyAt(Instant.now());
        return toPhoto(photoRepository.save(photo), currentUser.id());
    }

    @Transactional
    public PoiResponses.Photo updatePhoto(long poiId, long photoId, PoiRequests.PhotoUpdate request) {
        activePoi(poiId);
        PoiPhoto photo = photoRepository.findByIdAndPoiId(photoId, poiId)
                .orElseThrow(() -> new PoiContentNotFoundException("Photo"));
        requireOwner(photo.getContributorId());
        if (photo.getStatus() != PoiPhotoStatus.READY) {
            throw invalidUpload("Only finalized photos can be updated");
        }
        photo.setCaption(optionalText(request.caption(), 500));
        return toPhoto(photoRepository.save(photo), currentUser.id());
    }

    @Transactional
    public void deletePhoto(long poiId, long photoId) {
        activePoi(poiId);
        PoiPhoto photo = photoRepository.findByIdAndPoiId(photoId, poiId)
                .orElseThrow(() -> new PoiContentNotFoundException("Photo"));
        requireOwner(photo.getContributorId());
        if (photo.getStatus() != PoiPhotoStatus.DELETED) {
            photoStorage.delete(photo.getObjectKey());
            photo.setStatus(PoiPhotoStatus.DELETED);
            photoRepository.save(photo);
        }
    }

    public PoiResponses.Summary toSummary(PointOfInterest poi) {
        RatingAggregate aggregate = aggregate(poi.getId());
        return new PoiResponses.Summary(
                poi.getId(),
                poi.getName(),
                poi.getDescription(),
                poi.getLocation().getX(),
                poi.getLocation().getY(),
                poi.getOwnerId(),
                poi.getOwnerDisplayName(),
                poi.getOwnerId().equals(currentUser.id()),
                aggregate.average(),
                aggregate.count(),
                aggregate.userScore(),
                photos(poi.getId())
        );
    }

    private PoiResponses.Rating ratingResponse(long poiId) {
        RatingAggregate aggregate = aggregate(poiId);
        return new PoiResponses.Rating(aggregate.average(), aggregate.count(), aggregate.userScore());
    }

    private RatingAggregate aggregate(long poiId) {
        Object[] result = ratingRepository.aggregateForPoi(poiId);
        double average = ((Number) result[0]).doubleValue();
        long count = ((Number) result[1]).longValue();
        Integer userScore = ratingRepository.findByPoiIdAndUserId(poiId, currentUser.id())
                .map(PoiRating::getScore)
                .orElse(null);
        return new RatingAggregate(average, count, userScore);
    }

    private List<PoiResponses.Photo> photos(long poiId) {
        String userId = currentUser.id();
        return photoRepository.findByPoiIdAndStatusOrderByCreatedAtAsc(poiId, PoiPhotoStatus.READY)
                .stream()
                .map(photo -> toPhoto(photo, userId))
                .toList();
    }

    private PoiResponses.Photo toPhoto(PoiPhoto photo, String userId) {
        return new PoiResponses.Photo(
                photo.getId(),
                photo.getContributorId(),
                photo.getContributorDisplayName(),
                photo.getContributorId().equals(userId),
                photo.getPublicUrl(),
                photo.getContentType(),
                photo.getSizeBytes(),
                photo.getCaption(),
                photo.getCreatedAt()
        );
    }

    private PoiResponses.Comment toComment(PoiComment comment, String userId) {
        return new PoiResponses.Comment(
                comment.getId(),
                comment.getAuthorId(),
                comment.getAuthorDisplayName(),
                comment.getAuthorId().equals(userId),
                comment.getText(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private PointOfInterest activePoi(long id) {
        return poiRepository.findByIdAndDeletedFalse(id).orElseThrow(PoiNotFoundException::new);
    }

    private PointOfInterest ownedPoi(long id) {
        PointOfInterest poi = activePoi(id);
        requireOwner(poi.getOwnerId());
        return poi;
    }

    private PoiComment ownedComment(long poiId, long commentId) {
        PoiComment comment = commentRepository.findByIdAndPoiIdAndDeletedFalse(commentId, poiId)
                .orElseThrow(() -> new PoiContentNotFoundException("Comment"));
        requireOwner(comment.getAuthorId());
        return comment;
    }

    private void requireOwner(String ownerId) {
        if (!ownerId.equals(currentUser.id()) && !currentUser.isAdmin()) {
            throw new ForbiddenOperationException();
        }
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPoiDataException("INVALID_REQUEST", "Page must be non-negative and size must be 1 to 100");
        }
        return PageRequest.of(page, size, sort);
    }

    private Point point(Double longitude, Double latitude) {
        double validLongitude = requireLongitude(longitude);
        double validLatitude = requireLatitude(latitude);
        Point point = geometryFactory.createPoint(new Coordinate(validLongitude, validLatitude));
        point.setSRID(4326);
        return point;
    }

    private double requireLongitude(Double value) {
        if (value == null || !Double.isFinite(value) || value < -180 || value > 180) {
            throw invalidLocation("Longitude must be between -180 and 180");
        }
        return value;
    }

    private double requireLatitude(Double value) {
        if (value == null || !Double.isFinite(value) || value < -90 || value > 90) {
            throw invalidLocation("Latitude must be between -90 and 90");
        }
        return value;
    }

    private double requireNearbyRadius(Double value) {
        if (value == null || !Double.isFinite(value) || value <= 0 || value > MAX_NEARBY_RADIUS_METERS) {
            throw invalidLocation("Radius must be greater than 0 and at most 100000 meters");
        }
        return value;
    }

    private String requiredText(String value, String field, int maximumLength) {
        String normalized = optionalText(value, maximumLength);
        if (normalized == null || normalized.isBlank()) {
            throw new InvalidPoiDataException("INVALID_REQUEST", field + " is required");
        }
        return normalized;
    }

    private String optionalText(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new InvalidPoiDataException("INVALID_REQUEST", "Text exceeds maximum length of " + maximumLength);
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizePhotoType(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
        if (!PHOTO_TYPES.contains(normalized)) {
            throw invalidUpload("Photo must be JPEG, PNG, or WebP");
        }
        return normalized;
    }

    private long requirePhotoSize(Long sizeBytes) {
        if (sizeBytes == null || sizeBytes < 1 || sizeBytes > MAX_PHOTO_BYTES) {
            throw invalidUpload("Photo size must be between 1 byte and 10 MB");
        }
        return sizeBytes;
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw invalidUpload("Unsupported photo type");
        };
    }

    private InvalidPoiDataException invalidLocation(String message) {
        return new InvalidPoiDataException("INVALID_LOCATION", message);
    }

    private InvalidPoiDataException invalidUpload(String message) {
        return new InvalidPoiDataException("INVALID_UPLOAD", message);
    }

    private record RatingAggregate(double average, long count, Integer userScore) {
    }
}
