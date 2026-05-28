package my.mma.api.report.repository;

import my.mma.api.report.entity.ReportDedup;
import org.springframework.data.repository.CrudRepository;

public interface ReportDedupRepository extends CrudRepository<ReportDedup, String> {
    Iterable<ReportDedup> findAllByMessageId(String messageId);
}
