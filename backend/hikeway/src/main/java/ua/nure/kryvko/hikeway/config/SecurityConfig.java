package ua.nure.kryvko.hikeway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import ua.nure.kryvko.hikeway.model.UserRole;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) {
        http.oauth2ResourceServer(resourceServer -> {
            resourceServer.jwt(jwtDecoder -> jwtDecoder.jwtAuthenticationConverter(new KeycloakJwtConverter()));
        });

        http.sessionManagement(sessions -> {
            sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }).csrf(AbstractHttpConfigurer::disable);

        http.authorizeHttpRequests(requests -> {
            requests
                    .requestMatchers("/admin").hasRole(UserRole.ADMIN.toString())
                    .requestMatchers("/auth/signup", "/auth/signin").permitAll()
                    .anyRequest().authenticated();
        });

        return http.build();
    }
}
