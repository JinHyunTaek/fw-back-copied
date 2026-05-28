package my.mma.api.smtp.repository;

import my.mma.api.smtp.entity.JoinCode;
import org.springframework.data.repository.CrudRepository;

public interface JoinCodeRepository extends CrudRepository<JoinCode,String> {
}
