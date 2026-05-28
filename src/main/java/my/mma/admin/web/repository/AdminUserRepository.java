package my.mma.admin.web.repository;

import my.mma.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByRoleEqualsAndEmail(String role, String email);
}
