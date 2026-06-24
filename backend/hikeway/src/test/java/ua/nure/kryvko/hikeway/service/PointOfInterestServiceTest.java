package ua.nure.kryvko.hikeway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import ua.nure.kryvko.hikeway.exception.ForbiddenOperationException;
import ua.nure.kryvko.hikeway.exception.InvalidPoiDataException;
import ua.nure.kryvko.hikeway.model.PoiRating;
import ua.nure.kryvko.hikeway.model.PoiPhoto;
import ua.nure.kryvko.hikeway.model.PoiPhotoStatus;
import ua.nure.kryvko.hikeway.model.PoiComment;
import ua.nure.kryvko.hikeway.model.PointOfInterest;
import ua.nure.kryvko.hikeway.model.request.PoiRequests;
import ua.nure.kryvko.hikeway.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PointOfInterestServiceTest {
    private PointOfInterestRepository poiRepository;
    private PoiRatingRepository ratingRepository;
    private PoiCommentRepository commentRepository;
    private PoiPhotoRepository photoRepository;
    private RoutePoiRepository routePoiRepository;
    private CurrentUserService currentUser;
    private PoiPhotoStorage photoStorage;
    private PointOfInterestService service;

    @BeforeEach
    void setUp() {
        poiRepository = mock(PointOfInterestRepository.class);
        ratingRepository = mock(PoiRatingRepository.class);
        commentRepository = mock(PoiCommentRepository.class);
        photoRepository = mock(PoiPhotoRepository.class);
        routePoiRepository = mock(RoutePoiRepository.class);
        currentUser = mock(CurrentUserService.class);
        photoStorage = mock(PoiPhotoStorage.class);
        when(currentUser.id()).thenReturn("user-1");
        when(currentUser.displayName()).thenReturn("Hiker");
        when(ratingRepository.aggregateForPoi(anyLong())).thenReturn(new Object[]{0.0, 0L});
        when(ratingRepository.findByPoiIdAndUserId(anyLong(), anyString())).thenReturn(Optional.empty());
        when(photoRepository.findByPoiIdAndStatusOrderByCreatedAtAsc(anyLong(), any())).thenReturn(List.of());
        service = new PointOfInterestService(
                poiRepository,
                ratingRepository,
                commentRepository,
                photoRepository,
                routePoiRepository,
                currentUser,
                photoStorage
        );
    }

    @Test
    void createsPoiOwnedByCurrentUser() {
        AtomicReference<PointOfInterest> saved = new AtomicReference<>();
        when(poiRepository.save(any(PointOfInterest.class))).thenAnswer(invocation -> {
            PointOfInterest poi = invocation.getArgument(0);
            poi.setId(7L);
            saved.set(poi);
            return poi;
        });
        when(poiRepository.findByIdAndDeletedFalse(7L))
                .thenAnswer(invocation -> Optional.of(saved.get()));

        var response = service.create(new PoiRequests.Create(
                "Forest spring",
                "Fresh water",
                24.1,
                49.8
        ));

        assertEquals(7L, response.id());
        assertEquals("user-1", response.ownerId());
        assertEquals(24.1, response.longitude());
    }

    @Test
    void rejectsUpdateByNonOwner() {
        PointOfInterest poi = poi("other-user");
        when(poiRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(poi));

        assertThrows(
                ForbiddenOperationException.class,
                () -> service.update(7L, new PoiRequests.Update("Changed", null, null, null))
        );
        verify(poiRepository, never()).save(any());
    }

    @Test
    void ratingUpsertIsPerCurrentUser() {
        PointOfInterest poi = poi("owner");
        when(poiRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(poi));
        when(ratingRepository.aggregateForPoi(7L)).thenReturn(new Object[]{4.0, 1L});
        when(ratingRepository.findByPoiIdAndUserId(7L, "user-1"))
                .thenReturn(Optional.empty())
                .thenAnswer(invocation -> {
                    PoiRating rating = new PoiRating();
                    rating.setScore(4);
                    return Optional.of(rating);
                });

        var response = service.rate(7L, new PoiRequests.Rating(4));

        assertEquals(4, response.userRating());
        verify(ratingRepository).save(argThat(rating ->
                rating.getPoi() == poi && rating.getScore() == 4 && rating.getUserId().equals("user-1")
        ));
    }

    @Test
    void rejectsInvalidCoordinatesBeforePersistence() {
        assertThrows(
                InvalidPoiDataException.class,
                () -> service.create(new PoiRequests.Create("Place", "Description", 181.0, 49.8))
        );
        verify(poiRepository, never()).save(any());
    }

    @Test
    void createsPresignedPhotoUploadWithServerGeneratedKey() {
        PointOfInterest poi = poi("owner");
        when(poiRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(poi));
        when(photoRepository.save(any(PoiPhoto.class))).thenAnswer(invocation -> {
            PoiPhoto photo = invocation.getArgument(0);
            photo.setId(9L);
            return photo;
        });
        when(photoStorage.createUpload(anyString(), eq("image/jpeg"), eq(1024L)))
                .thenReturn(new PoiPhotoStorage.PresignedUpload(
                        "http://storage/upload",
                        Instant.parse("2026-06-24T12:15:00Z")
                ));

        var response = service.createPhotoUpload(
                7L,
                new PoiRequests.PhotoUpload("image/jpeg", 1024L)
        );

        assertEquals(9L, response.photoId());
        assertEquals("http://storage/upload", response.uploadUrl());
        verify(photoStorage).createUpload(
                argThat(key -> key.startsWith("pois/7/") && key.endsWith(".jpg")),
                eq("image/jpeg"),
                eq(1024L)
        );
    }

    @Test
    void rejectsFinalizedPhotoWhenStoredMetadataDoesNotMatch() {
        PointOfInterest poi = poi("owner");
        PoiPhoto photo = new PoiPhoto();
        photo.setId(9L);
        photo.setPoi(poi);
        photo.setContributorId("user-1");
        photo.setObjectKey("pois/7/photo.jpg");
        photo.setContentType("image/jpeg");
        photo.setSizeBytes(1024L);
        photo.setStatus(PoiPhotoStatus.PENDING);
        when(poiRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(poi));
        when(photoRepository.findByIdAndPoiId(9L, 7L)).thenReturn(Optional.of(photo));
        when(photoStorage.inspect(photo.getObjectKey()))
                .thenReturn(new PoiPhotoStorage.StoredObject("image/jpeg", 100L));

        assertThrows(
                InvalidPoiDataException.class,
                () -> service.finalizePhoto(7L, new PoiRequests.PhotoFinalize(9L, null))
        );

        verify(photoStorage).delete(photo.getObjectKey());
        assertEquals(PoiPhotoStatus.DELETED, photo.getStatus());
    }

    @Test
    void rejectsCommentEditByAnotherContributor() {
        PointOfInterest poi = poi("owner");
        PoiComment comment = new PoiComment();
        comment.setId(3L);
        comment.setPoi(poi);
        comment.setAuthorId("other-user");
        when(poiRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(poi));
        when(commentRepository.findByIdAndPoiIdAndDeletedFalse(3L, 7L))
                .thenReturn(Optional.of(comment));

        assertThrows(
                ForbiddenOperationException.class,
                () -> service.updateComment(7L, 3L, new PoiRequests.Comment("Changed"))
        );
        verify(commentRepository, never()).save(any());
    }

    private PointOfInterest poi(String ownerId) {
        PointOfInterest poi = new PointOfInterest();
        poi.setId(7L);
        poi.setOwnerId(ownerId);
        poi.setOwnerDisplayName("Owner");
        poi.setName("Forest spring");
        poi.setDescription("Fresh water");
        var point = new GeometryFactory().createPoint(new Coordinate(24.1, 49.8));
        point.setSRID(4326);
        poi.setLocation(point);
        poi.setCreatedAt(Instant.now());
        poi.setUpdatedAt(Instant.now());
        return poi;
    }
}
