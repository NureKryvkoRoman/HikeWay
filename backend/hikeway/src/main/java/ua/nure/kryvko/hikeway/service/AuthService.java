package ua.nure.kryvko.hikeway.service;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.nure.kryvko.hikeway.exception.InvalidUserDataException;
import ua.nure.kryvko.hikeway.exception.KeycloakException;
import ua.nure.kryvko.hikeway.exception.UserAlreadyExistsException;
import ua.nure.kryvko.hikeway.model.UserEntity;
import ua.nure.kryvko.hikeway.model.request.SignUpRequest;
import ua.nure.kryvko.hikeway.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
public class AuthService {
    private final Keycloak keycloak;
    private final String realm;
    private final UserRepository userRepository;

    @Autowired
    public AuthService(Keycloak keycloak, @Value("${keycloak.target-realm}") String realm, UserRepository userRepository) {
        this.keycloak = keycloak;
        this.realm = realm;
        this.userRepository = userRepository;
    }

    public UserEntity signUp(SignUpRequest request) {
        var password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(request.password());
        password.setTemporary(false);

        var user = new UserRepresentation();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setCredentials(List.of(password));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmailVerified(true); //FIXME: skipped for simplicity now, enable later
        user.setEnabled(true);

        try (var response = keycloak.realm(realm).users().create(user)) {
            switch (response.getStatus()) {
                case 201 -> {
                    String userId = CreatedResponseUtil.getCreatedId(response);

                    var userEntity = new UserEntity(
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getUsername(),
                            userId
                    );

                    try {
                        return userRepository.save(userEntity);
                    } catch (Exception e) {
                        // Rollback in Keycloak
                        var delResponse = keycloak.realm(realm).users().delete(userId);
                        delResponse.close();
                        throw e;
                    }
                }
                case 409 -> {
                    throw new UserAlreadyExistsException("User already exists " + response.readEntity(String.class));
                }
                case 400 -> {
                    throw new InvalidUserDataException("Invalid user data " + response.readEntity(String.class));
                }
            }
            throw new KeycloakException("Keycloak error: " + response.readEntity(String.class));
        }
    }
}
