package my.mma.api.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class FcmTokenUpdateService {

    private final UserRepository userRepository;

    @Transactional
    public void updateUserFcmToken(String email, String fcmToken){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_USER_FOUND_400));
        user.updateFcmToken(fcmToken);
    }

}
