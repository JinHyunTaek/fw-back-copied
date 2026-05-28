package my.mma.api.announcement.repository;

import my.mma.api.announcement.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnounceRepository extends JpaRepository<Announcement, Long> {
}
