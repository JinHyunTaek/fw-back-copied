package my.mma.api.security.repository;

import my.mma.api.security.entity.PasswordResetToken;
import org.springframework.data.repository.CrudRepository;

public interface PasswordResetTokenRepository extends CrudRepository<PasswordResetToken, String> {
}
