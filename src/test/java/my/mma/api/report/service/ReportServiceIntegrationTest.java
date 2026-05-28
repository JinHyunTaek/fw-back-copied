//package my.mma.api.report.service;
//
//import jakarta.persistence.EntityManager;
//import my.mma.api.report.dto.ReportRequest;
//import my.mma.api.user.entity.User;
//import my.mma.api.user.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import static my.mma.api.report.entity.ReportCategory.SWEAR_WORD;
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Import(ReportService.class)
//@DataJpaTest
//@ActiveProfiles("test")
//class ReportServiceIntegrationTest {
//
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private ReportService reportService;
//    @Autowired
//    private EntityManager em;
//
//    private final int threadCount = 20;
//    private final String nickname = "nickname123";
//    private final String email = "email123@google.com";
//    private final String password = "pwd123";
//    private final String messageId = UUID.randomUUID().toString();
//    private final String messageSnapshot = "kind of bad word";
//
//    private User userToReported;
//    private List<User> reporters = new ArrayList<>();
//
//    @BeforeEach
//    void setupUsers() {
//        userToReported = userRepository.save(User.builder()
//                .nickname(nickname)
//                .email(email)
//                .point(0)
//                .role("ROLE_USER")
//                .password(password)
//                .build());
//        for (int i = 0; i < threadCount; i++) {
//            reporters.add(userRepository.save(User.builder()
//                    .nickname(nickname + i)
//                    .email(email + i)
//                    .point(0)
//                    .role("ROLE_USER")
//                    .password(password)
//                    .build()));
//        }
//    }
//
//    @Test
//    @DisplayName("하나의 유저가 하나의 채팅 메시지 신고 시 true 반환")
//    void returnTrueIfOneUserReportsOneMessage() {
//        //given
//        ReportRequest reportRequest = new ReportRequest(SWEAR_WORD, messageId, messageSnapshot, userToReported.getId());
//
//        //when
//        boolean res = reportService.report(email, reportRequest);
//
//        //then
//        assertThat(res).isTrue();
////        ReportedMessage saved = reportedMessageRepository.findByMessageIdWithLock(messageId).orElseThrow();
////        assertThat(messageReporterHistoryRepository.count()).isEqualTo(1);
////        assertThat(saved.getMessageId()).isEqualTo(messageId);
//    }
//
//    @Test
//    @DisplayName("여러 유저가 하나의 채팅 메시지 신고 시 신고 횟수 증가")
//    void increaseReportedCountIfManyUsersReportOneMessage() {
//        //given && when
//        for (User user : reporters) {
//            ReportRequest reportRequest = new ReportRequest(SWEAR_WORD, messageId, messageSnapshot, userToReported.getId());
//            boolean res = reportService.report(user.getEmail(), reportRequest);
//            em.flush();
//            em.clear();
//            assertThat(res).isTrue();
//        }
//
//        //then
////        ReportedMessage findReportedMessage = reportedMessageRepository.findByMessageIdWithLock(messageId).get();
////        assertThat(messageReporterHistoryRepository.count()).isEqualTo(reporters.size());
////        assertThat(findReportedMessage.getReportedCount()).isEqualTo(reporters.size());
////        assertThat(reportedMessageRepository.count()).isEqualTo(1);
//    }
//
//    @Test
//    @DisplayName("유저가 채팅 메시지 중복 신고 시 false 반환")
//    void duplicateReport_shouldReturnFalse() {
//        //given
//        ReportRequest reportRequest = new ReportRequest(SWEAR_WORD, messageId, messageSnapshot, userToReported.getId());
//
//        //when
//        boolean res1 = reportService.report(email, reportRequest);
//        em.flush();
//        em.clear();
//        boolean res2 = reportService.report(email, reportRequest);
//
//        //then
//        assertThat(res1).isTrue();
//        assertThat(res2).isFalse();
//
//    }
//
////    @Test
//    @DisplayName("여러 유저가 동시에 신고해도 reportedCount 정확히 증가")
//    void concurrentReport_shouldIncreaseCountCorrectly() throws Exception {
//
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        // given
//
//        // when
//        for (User reporter : reporters) {
//            executorService.submit(() -> {
//                try {
//                    ReportRequest request = new ReportRequest(
//                            SWEAR_WORD,
//                            messageId,
//                            messageSnapshot,
//                            userToReported.getId()
//                    );
//                    reportService.report(reporter.getEmail(), request);
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//        latch.await();
//        executorService.shutdown();
//
//        // then
////        ReportedMessage result =
////                reportedMessageRepository.findByMessageIdWithLock(messageId).orElseThrow();
////        assertThat(result.getReportedCount()).isEqualTo(threadCount);
//    }
//
//
//}