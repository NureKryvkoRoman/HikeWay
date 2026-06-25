package ua.nure.kryvko.hikeway.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ua.nure.kryvko.hikeway.model.UserRole;

@Component
public class CurrentUserService {
    public String id() {
        return jwt().getSubject();
    }

    public String displayName() {
        Jwt jwt = jwt();
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        String name = jwt.getClaimAsString("name");
        return name == null || name.isBlank() ? jwt.getSubject() : name;
    }

    public boolean isAdmin() {
        String adminAuthority = "ROLE_" + UserRole.ADMIN;
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> adminAuthority.equals(authority.getAuthority()));
    }

    private Jwt jwt() {
        return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
