package my.mma.api.user.repository;

import my.mma.api.user.entity.PunishedUser;
import org.springframework.data.repository.CrudRepository;

public interface PunishedUserRepository extends CrudRepository<PunishedUser, Long> {
}
