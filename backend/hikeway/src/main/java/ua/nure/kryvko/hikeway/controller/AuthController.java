package ua.nure.kryvko.hikeway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.nure.kryvko.hikeway.model.UserEntity;
import ua.nure.kryvko.hikeway.model.request.SignUpRequest;
import ua.nure.kryvko.hikeway.service.AuthService;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserEntity> signUp(@RequestBody SignUpRequest request) {
        var user = authService.signUp(request);
        return ResponseEntity.ok(user);
    }
}
