package ua.nure.kryvko.hikeway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.nure.kryvko.hikeway.model.request.PoiRequests;
import ua.nure.kryvko.hikeway.model.response.PageResponse;
import ua.nure.kryvko.hikeway.model.response.PoiResponses;
import ua.nure.kryvko.hikeway.service.PointOfInterestService;

@RestController
@RequestMapping("/pois")
public class PointOfInterestController {
    private final PointOfInterestService service;

    public PointOfInterestController(PointOfInterestService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PoiResponses.Summary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Double minLongitude,
            @RequestParam(required = false) Double minLatitude,
            @RequestParam(required = false) Double maxLongitude,
            @RequestParam(required = false) Double maxLatitude
    ) {
        return service.list(page, size, minLongitude, minLatitude, maxLongitude, maxLatitude);
    }

    @GetMapping("/{id}")
    public PoiResponses.Detail get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<PoiResponses.Detail> create(@RequestBody PoiRequests.Create request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PatchMapping("/{id}")
    public PoiResponses.Detail update(@PathVariable long id, @RequestBody PoiRequests.Update request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/rating")
    public PoiResponses.Rating rate(@PathVariable long id, @RequestBody PoiRequests.Rating request) {
        return service.rate(id, request);
    }

    @DeleteMapping("/{id}/rating")
    public PoiResponses.Rating removeRating(@PathVariable long id) {
        return service.removeRating(id);
    }

    @GetMapping("/{id}/comments")
    public PageResponse<PoiResponses.Comment> comments(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return service.comments(id, page, size);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<PoiResponses.Comment> addComment(
            @PathVariable long id,
            @RequestBody PoiRequests.Comment request
    ) {
        return ResponseEntity.status(201).body(service.addComment(id, request));
    }

    @PatchMapping("/{id}/comments/{commentId}")
    public PoiResponses.Comment updateComment(
            @PathVariable long id,
            @PathVariable long commentId,
            @RequestBody PoiRequests.Comment request
    ) {
        return service.updateComment(id, commentId, request);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable long id, @PathVariable long commentId) {
        service.deleteComment(id, commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/photos/uploads")
    public PoiResponses.Upload createPhotoUpload(
            @PathVariable long id,
            @RequestBody PoiRequests.PhotoUpload request
    ) {
        return service.createPhotoUpload(id, request);
    }

    @PostMapping("/{id}/photos")
    public ResponseEntity<PoiResponses.Photo> finalizePhoto(
            @PathVariable long id,
            @RequestBody PoiRequests.PhotoFinalize request
    ) {
        return ResponseEntity.status(201).body(service.finalizePhoto(id, request));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable long id, @PathVariable long photoId) {
        service.deletePhoto(id, photoId);
        return ResponseEntity.noContent().build();
    }
}
