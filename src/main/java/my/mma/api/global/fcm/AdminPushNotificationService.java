package my.mma.api.global.fcm;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminPushNotificationService {

    private final UserRepository userRepository;
    private final FcmMessageService fcmMessageService;

    public void sendNotificationToAdmin(String message) {
        try {
            User admin = userRepository.findByRoleEquals("ROLE_ADMIN");
            fcmMessageService.sendMessage(Message.builder()
                    .setToken(admin.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle("예외 발생")
                            .setBody(message)
                            .build()
                    ).build());
        } catch (Exception e) {
            log.error("Error while sending notification to admin, e=", e);
        }
    }

}
