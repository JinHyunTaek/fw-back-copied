package my.mma.api.status.repository;

import my.mma.api.status.entity.AppStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppStatusRepository extends JpaRepository<AppStatus, Long> {
}
