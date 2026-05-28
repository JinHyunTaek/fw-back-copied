package my.mma.api.report.repository;

import my.mma.api.report.entity.ReportedMessageData;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface ReportedMessageDataRepository extends ListCrudRepository<ReportedMessageData, String> {
    List<ReportedMessageData> findAllByReportedUserId(Long reportedUserId);
}
