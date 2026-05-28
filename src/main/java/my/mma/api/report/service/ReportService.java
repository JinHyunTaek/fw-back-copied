package my.mma.api.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.report.dto.ReportRequest;
import my.mma.api.report.entity.ReportDedup;
import my.mma.api.report.entity.ReportedMessageData;
import my.mma.api.report.repository.ReportDedupRepository;
import my.mma.api.report.repository.ReportedMessageDataRepository;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportedMessageDataRepository reportedMessageDataRepository;
    private final ReportDedupRepository reportDedupRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean report(String email, ReportRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_USER_FOUND_400));
        if (!userRepository.existsById(request.reportedUserId()))
            throw new CustomException(ErrorCode.NO_SUCH_USER_FOUND_400);
        if (reportDedupRepository.existsById(user.getId() + ":" + request.messageId())) {
            return false;
        }
        reportDedupRepository.save(ReportDedup.of(user.getId(), request.messageId(), request.reportCategory()));
        reportedMessageDataRepository.findById(request.messageId())
                .ifPresentOrElse(
                        data -> {
                            data.increaseReportedCount();
                            reportedMessageDataRepository.save(data);
                        },
                        () -> reportedMessageDataRepository.save(ReportedMessageData.of(request))
                );
        return true;
    }

}
