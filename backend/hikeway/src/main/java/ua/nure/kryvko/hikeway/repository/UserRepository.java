package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.nure.kryvko.hikeway.model.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
