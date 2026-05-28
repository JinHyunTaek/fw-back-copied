package my.mma.api.game.repository;

import my.mma.api.game.entity.GameAttempt;
import org.springframework.data.repository.CrudRepository;

public interface GameAttemptRepository extends CrudRepository<GameAttempt,Long> {
}
