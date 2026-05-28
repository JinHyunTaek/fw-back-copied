package my.mma.api.user.scheduler;

import lombok.RequiredArgsConstructor;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class UserRankingInitializeScheduler {

    private final UserRepository userRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 1 1,1 *")
    public void initUserRanking(){
        userRepository.initializeBetEarnedPoints();
    }

}
