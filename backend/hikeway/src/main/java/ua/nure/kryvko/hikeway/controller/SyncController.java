package ua.nure.kryvko.hikeway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.nure.kryvko.hikeway.model.request.SyncRequest;
import ua.nure.kryvko.hikeway.model.response.SyncResponse;
import ua.nure.kryvko.hikeway.service.DataSyncService;

import java.util.UUID;

@RestController
public class SyncController {
    private final DataSyncService syncService;

    public SyncController(DataSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync")
    public ResponseEntity<SyncResponse> synchronize(@RequestBody SyncRequest request) {
        return ResponseEntity.ok(syncService.synchronize(request));
    }

    @PostMapping("/routes/{clientId}/publish")
    public ResponseEntity<SyncResponse.RouteChange> publish(@PathVariable UUID clientId) {
        return ResponseEntity.ok(syncService.setPublished(clientId, true));
    }

    @PostMapping("/routes/{clientId}/unpublish")
    public ResponseEntity<SyncResponse.RouteChange> unpublish(@PathVariable UUID clientId) {
        return ResponseEntity.ok(syncService.setPublished(clientId, false));
    }
}
