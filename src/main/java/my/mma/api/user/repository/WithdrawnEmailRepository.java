package my.mma.api.user.repository;

import my.mma.api.user.entity.WithdrawnUserEmail;
import org.springframework.data.repository.CrudRepository;

public interface WithdrawnEmailRepository extends CrudRepository<WithdrawnUserEmail, String> {
}
