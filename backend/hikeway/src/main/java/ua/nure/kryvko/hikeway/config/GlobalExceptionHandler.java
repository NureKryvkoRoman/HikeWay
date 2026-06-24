package ua.nure.kryvko.hikeway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.nure.kryvko.hikeway.exception.InvalidRouteGeometryException;
import ua.nure.kryvko.hikeway.exception.InvalidUserDataException;
import ua.nure.kryvko.hikeway.exception.KeycloakException;
import ua.nure.kryvko.hikeway.exception.RouteNotFoundException;
import ua.nure.kryvko.hikeway.exception.UserAlreadyExistsException;
import ua.nure.kryvko.hikeway.exception.SyncBatchTooLargeException;
import ua.nure.kryvko.hikeway.model.response.ApiError;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUserExists() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("USER_ALREADY_EXISTS", "User already exists"));
    }

    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<ApiError> handleInvalidUserData(InvalidUserDataException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("INVALID_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(KeycloakException.class)
    public ResponseEntity<ApiError> handleKC() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of("AUTH_SERVICE_ERROR", "Auth service error"));
    }

    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<ApiError> handleRouteNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("ROUTE_NOT_FOUND", "Route not found"));
    }

    @ExceptionHandler(InvalidRouteGeometryException.class)
    public ResponseEntity<ApiError> handleInvalidRouteGeometry(InvalidRouteGeometryException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("INVALID_GEOMETRY", exception.getMessage()));
    }

    @ExceptionHandler(SyncBatchTooLargeException.class)
    public ResponseEntity<ApiError> handleSyncBatchTooLarge(SyncBatchTooLargeException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiError.of("SYNC_BATCH_TOO_LARGE", exception.getMessage()));
    }
}
