package my.mma.admin.web.service.fighter;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.dto.fighter.AdminFighterResponseForUpdate;
import my.mma.admin.web.dto.fighter.AdminFighterUpdateRequest;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fighter.dto.RankersPerCategory;
import my.mma.api.fighter.dto.RankersPerCategory.RankerDto;
import my.mma.api.fighter.dto.RankersPerCategory.RankingCategory;
import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.fightevent.dto.CurrentEventDto.CurrentFighterFightEventDto;
import my.mma.api.fightevent.store.CurrentEventStore;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static my.mma.api.global.redis.prefix.RedisKeyPrefix.RANKERS_KEY_PREFIX;

@Service
@Slf4j
@Transactional(readOnly = true)
public class AdminFighterService {

    private final WebClient webClient;
    private final FighterRepository fighterRepository;
    private final RedisUtils<RankersPerCategory> rankerRedisUtils;
    private final CurrentEventStore currentEventStore;

    public AdminFighterService(
            @Value("${flask.uri}") String flaskUrl,
            FighterRepository fighterRepository,
            RedisUtils<RankersPerCategory> rankers,
            CurrentEventStore currentEventStore
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(60));
        this.webClient = WebClient.builder()
                .baseUrl(flaskUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.fighterRepository = fighterRepository;
        this.rankerRedisUtils = rankers;
        this.currentEventStore = currentEventStore;
    }

    public Page<FighterDto> search(String name, Pageable pageable) {
        Page<Fighter> fighters = fighterRepository.searchByNameOrKoreanName(name, pageable);
        //                    fighterDto.setHeadshotUrl(s3ImgService.generateFighterHeadshotUrl(fighterDto.getName()));
        return fighters.map(
                FighterDto::toDto
        );
    }

    public AdminFighterResponseForUpdate detail(Long fighterId) {
        Fighter fighter = fighterRepository.findById(fighterId).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_FIGHTER_CONFIGURED_400)
        );
        return AdminFighterResponseForUpdate.toDto(fighter);
    }

    @Loggable
    @Transactional
    public void updateRanking() {
        List<Fighter> prevRankedFighters = fighterRepository.findFightersByRankingIsNotNull();
        prevRankedFighters.forEach(fighter -> fighter.updateRanking(null));
        RankersPerCategory rankersDto = webClient.get()
                .uri("/fighter_ranking")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(RankersPerCategory.class)
                .block();
        Map<RankingCategory, List<RankerDto>> rankersPerCategory = new HashMap<>();
        if (rankersDto != null) {
            Map<String, Fighter> fighterMap = fighterRepository.findAllByNameIn(
                            rankersDto.rankers().stream().map(RankerDto::getName).toList())
                    .stream().collect(Collectors.toMap(Fighter::getName, f -> f));
            rankersDto.rankers().forEach(
                    rankerDto -> {
                        Fighter fighter = fighterMap.get(rankerDto.getName());
                        if (fighter != null) {
                            if (!rankerDto.getCategory().name().contains("POUND_FOR_POUND"))
                                fighter.updateRanking(rankerDto.getRanking());
                            rankerDto.updateFromFighter(fighter);
//                                    s3ImgService.generateFighterHeadshotUrl(rankerDto.getName()));
                            List<RankerDto> rankerDtos = rankersPerCategory.get(rankerDto.getCategory());
                            if (rankerDtos == null)
                                rankerDtos = new ArrayList<>();
                            rankerDtos.add(rankerDto);
                            rankersPerCategory.put(rankerDto.getCategory(), rankerDtos);
                        } else {
                            log.error("cannot find ranker name={}", rankerDto.getName());
                        }
                    }
            );
            for (Map.Entry<RankingCategory, List<RankerDto>> entry : rankersPerCategory.entrySet()) {
                this.rankerRedisUtils.updateData(RANKERS_KEY_PREFIX.getPrefix() + entry.getKey().name(), new RankersPerCategory(entry.getValue()));
            }
        } else
            throw new CustomException(ErrorCode.SERVER_ERROR_500, "ranker data is null");
    }

//    public void updateImage(String fighterName, String nameUrl) {
//        try {
//            webClient.put()
//                    .uri("/fighter_image")
//                    .accept(MediaType.APPLICATION_JSON)
//                    .bodyValue(Map.of("fighterName", fighterName.replace(' ', '-'), "fighterNameUrl", nameUrl))
//                    .retrieve()
//                    .bodyToMono(Void.class)
//                    .block();
//            s3ImgService.evictFighterImgCache(fighterName);
//        } catch (Exception e) {
//            throw new CustomException(ErrorCode.SERVER_ERROR_500, "error while updating fighter image, e=" + e.getMessage());
//        }
//    }

    @Transactional
    public void updateFighter(Long id, AdminFighterUpdateRequest request) {
        Fighter fighter = fighterRepository.findById(id).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_FIGHTER_CONFIGURED_400)
        );
        fighter.updateFighterForAdmin(
                request.name(),
                request.koreanName(),
                request.nickname(),
                FightRecord.builder()
                        .win(request.win())
                        .loss(request.loss())
                        .draw(request.draw())
                        .build(),
                request.reach(),
                request.weight(),
                request.height(),
                request.birthday(),
                request.nationality()
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                syncCurrentEventCache(fighter);
            }
        });
    }

    private void syncCurrentEventCache(Fighter fighter) {
        currentEventStore.mutate(event -> {
            boolean changed = false;
            for (CurrentFighterFightEventDto ffe : event.getFighterFightEvents()) {
                if (fighter.getId().equals(ffe.getWinner().getId())) {   // ← NPE 방어
                    ffe.updateFighterInfo(fighter, true);
                    changed = true;
                } else if (fighter.getId().equals(ffe.getLoser().getId())) {
                    ffe.updateFighterInfo(fighter, false);
                    changed = true;
                }
                if (changed)
                    break;
            }
            return changed ? event : null;
        });
    }
}
