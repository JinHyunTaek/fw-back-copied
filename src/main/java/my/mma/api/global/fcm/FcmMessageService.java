package my.mma.api.global.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FcmMessageService {

    public static final int BATCH_SIZE = 500;

    // sync & http request * n회 => 매우 느림
    public void sendMessage(Message message) {
        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.error("exception while sending each fcm notification messages. e=", e);
        }
    }

    // async & http request (messages size / BATCH_SIZE(500))회 & 서로 다른 푸시 메시지 보낼 수 있음 (빠름)
    public void sendEach(List<Message> messages) {
        for (int i = 0; i < messages.size(); i += BATCH_SIZE) {
            try {
                int end = Math.min(i + BATCH_SIZE, messages.size());
                List<Message> batch = messages.subList(i, end);
                FirebaseMessaging.getInstance().sendEach(batch);
            } catch (FirebaseMessagingException e) {
                log.error("exception while sending each fcm notification messages. i={}, e=", i, e);
            }
        }
    }

    // async & http request (messages size / BATCH_SIZE(500))회 & 같은 푸시 메시지만 보냄 (빠름)
    public void sendEachForMulticast(MulticastMessage message) {
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        log.warn("FCM multicast failed at index {}. e=", i, responses.get(i).getException());
                    }
                }
            }
        } catch (FirebaseMessagingException e) {
            log.error("exception while sending multicast fcm notification messages. e=", e);
        }
    }

}
