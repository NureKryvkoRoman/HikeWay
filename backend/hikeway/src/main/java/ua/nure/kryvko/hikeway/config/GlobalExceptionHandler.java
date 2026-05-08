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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<String> handleUserExists() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("User already exists");
    }

    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<String> handleInvalidUserData() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid user data");
    }

    @ExceptionHandler(KeycloakException.class)
    public ResponseEntity<String> handleKC() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Auth service error");
    }

    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<String> handleRouteNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Route not found");
    }

    @ExceptionHandler(InvalidRouteGeometryException.class)
    public ResponseEntity<String> handleInvalidRouteGeometry(InvalidRouteGeometryException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }
}
