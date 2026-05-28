package my.mma.api.bet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.bet.dto.BetResponse;
import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.repository.BetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserRecentBetHistoryService {

    private final BetRepository betRepository;

    public List<BetResponse> userBetHistory(Long userId) {
        List<Long> recentFightEventIdsUserBetted = betRepository.findRecentCompletedEventIdsUserBetted(userId, PageRequest.of(0, 3));
        List<Long> recentUserBetIds = betRepository.findBetIdsByUserIdAndEventIds(userId, recentFightEventIdsUserBetted);
        List<Bet> recentBets = betRepository.findWithDetails(recentUserBetIds);
        Map<String, List<Bet>> betsByEvent =
                recentBets.stream()
                        .collect(Collectors.groupingBy(
                                bet -> bet.getFightEvent().getName(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));
        List<BetResponse> betResponseList = new ArrayList<>();
        for (Map.Entry<String, List<Bet>> entry : betsByEvent.entrySet()) {
            betResponseList.add(BetResponse
                    .builder()
                    .eventName(entry.getKey())
                    .singleBets(entry.getValue().stream().map(
                            BetResponse.SingleBetResponse::toDto).toList())
                    .build());
        }
        return betResponseList;
    }

}
