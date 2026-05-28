package my.mma.admin.web.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.dto.report.AdminActivateReportRequest;
import my.mma.admin.web.dto.report.AdminReportResponse;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.report.entity.ReportCategory;
import my.mma.api.report.entity.ReportedMessageData;
import my.mma.api.report.repository.ReportDedupRepository;
import my.mma.api.report.repository.ReportedMessageDataRepository;
import my.mma.api.user.entity.PunishedUser;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.PunishedUserRepository;
import my.mma.api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AdminReportService {

    private final ReportedMessageDataRepository reportedMessageRepository;
    private final ReportDedupRepository reportDedupRepository;
    private final PunishedUserRepository punishedUserRepository;
    private final UserRepository userRepository;

    public Page<AdminReportResponse> getReports(Pageable pageable) {
        List<ReportedMessageData> reportedMessages = reportedMessageRepository.findAll();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), reportedMessages.size());
        List<ReportedMessageData> pageContent = reportedMessages.subList(start, end);

        List<Long> reportedUserIds = pageContent.stream().map(ReportedMessageData::getReportedUserId).toList();
        Map<Long, String> userNicknameMap = userRepository.findAllById(reportedUserIds)
                .stream().collect(Collectors.toMap(User::getId, User::getNickname));

        List<AdminReportResponse> responses = pageContent.stream()
                .map(message -> AdminReportResponse.builder()
                        .id(message.getMessageId())
                        .messageSnapshot(message.getMessageSnapshot())
                        .reportedCount(message.getReportedCount())
                        .reportedUserId(message.getReportedUserId())
                        .reportedUserNickname(userNicknameMap.get(message.getReportedUserId()))
                        .reportedDate(message.getFirstReportedDate())
                        .reportCategory(getDominantCategory(message.getMessageId()))
                        .build())
                .toList();

        return new PageImpl<>(responses, pageable, reportedMessages.size());
    }

    private ReportCategory getDominantCategory(String messageId) {
        Map<ReportCategory, Long> counts = new HashMap<>();
        reportDedupRepository.findAllByMessageId(messageId)
                .forEach(d -> counts.merge(d.getReportCategory(), 1L, Long::sum));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ReportCategory.OTHER);
    }

    @Transactional
    public void activateReport(AdminActivateReportRequest request) {
        ReportedMessageData reportedMessage = reportedMessageRepository.findById(request.messageId())
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
        User reportedUser = userRepository.findById(reportedMessage.getReportedUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
        if (!punishedUserRepository.existsById(reportedUser.getId()))
            punishedUserRepository.save(PunishedUser.builder()
                    .userId(reportedUser.getId())
                    .reportCategory(request.category())
                    .expiration(Duration.ofDays(7).toSeconds())
                    .build());

        List<ReportedMessageData> userMessages = reportedMessageRepository.findAllByReportedUserId(reportedUser.getId());
        userMessages.forEach(m -> reportDedupRepository.deleteAll(reportDedupRepository.findAllByMessageId(m.getMessageId())));
        reportedMessageRepository.deleteAll(userMessages);
    }

    @Transactional
    public void deactivateReport(String messageId) {
        reportDedupRepository.deleteAll(reportDedupRepository.findAllByMessageId(messageId));
        reportedMessageRepository.deleteById(messageId);
    }

}
