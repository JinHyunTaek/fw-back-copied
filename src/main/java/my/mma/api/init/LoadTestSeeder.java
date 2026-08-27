package my.mma.api.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.bet.entity.FightPickCount;
import my.mma.api.bet.repository.FightPickCountRepository;
import my.mma.api.fighter.entity.Country;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.CardStartDateTimeInfo;
import my.mma.api.fightevent.entity.property.FightWeight;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import my.mma.api.security.JWTUtil;
import my.mma.api.security.dto.JwtCrateDto;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 부하 테스트용 데이터 부트스트랩. loadtest.seed=true 일 때만 실행된다.
 * <p>
 * 측정하려는 건 "서로 다른 사용자들이 같은 경기에 동시에 픽할 때의 경합"이므로
 * 사용자 N명과 각자의 accessToken 이 필요하다. 토큰은 JWT(서명된 문자열)라
 * 로그인 API 를 거치지 않고 발급해도 동일하게 유효하다 — 로그인 비용이 측정에 섞이지 않는다.
 * <p>
 * dev 는 ddl-auto=create-drop 이라 앱을 다시 띄우면 DB 가 비므로, 매번 깨끗한 상태에서 반복 측정할 수 있다.
 */
@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "loadtest.seed", havingValue = "true")
public class LoadTestSeeder {

    private static final int USER_COUNT = 1_000;
    private static final int USER_POINT = 100_000;
    private static final int FIGHT_COUNT = 12;   // 실제 UFC 카드 규모
    private static final Path CSV_PATH = Path.of("build", "loadtest-users.csv");

    @Value("${spring.jwt.access.expiration}")
    private Long accessExpireMs;

    private final UserRepository userRepository;
    private final FighterRepository fighterRepository;
    private final FightEventRepository fightEventRepository;
    private final FightPickCountRepository fightPickCountRepository;
    private final RedisUtils<CurrentEventDto> currentEventRedisUtils;
    private final JWTUtil jwtUtil;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        FightEvent event = seedEvent();
        seedPickCounts(event);
        seedUsersAndTokens();

        // predict() 가 Redis 의 CURRENT_EVENT 를 읽어 검증하므로 반드시 채워야 한다
        currentEventRedisUtils.saveData(RedisKey.CURRENT_EVENT.getKey(), CurrentEventDto.toDto(event));

        log.info("""

                        ===== 부하 테스트 데이터 준비 완료 =====
                        eventId          : {}
                        fighterFightEvent: {}
                        users            : {}명 (point {})
                        tokens           : {}
                        =======================================""",
                event.getId(), fightIds(event), USER_COUNT, USER_POINT, CSV_PATH.toAbsolutePath());
    }

    /** 마감 검증(isThisWeekend · 시작시각 경과)에 걸리지 않도록 2주 뒤로 잡는다. */
    private FightEvent seedEvent() {
        LocalDate eventDate = LocalDate.now().plusWeeks(2);
        FightEvent event = FightEvent.builder()
                .name("LOADTEST 100")
                .location("Las Vegas")
                .eventDate(eventDate)
                .displayDate(eventDate)
                .mainCardDateTimeInfo(cardTime(eventDate, 12, 0))
                .prelimCardDateTimeInfo(cardTime(eventDate, 10, 0))
                .earlyCardDateTimeInfo(cardTime(eventDate, 8, 0))
                .mainCardCnt(FIGHT_COUNT)
                .prelimCardCnt(0)
                .earlyCardCnt(0)
                .completed(false)
                .build();

        for (int i = 0; i < FIGHT_COUNT; i++) {
            Fighter winner = fighterRepository.save(fighter("Load Winner " + i, "부하위너" + i));
            Fighter loser = fighterRepository.save(fighter("Load Loser " + i, "부하루저" + i));
            event.getFighterFightEvents().add(FighterFightEvent.builder()
                    .fightEvent(event)
                    .winner(winner)
                    .loser(loser)
                    .fightWeight(FightWeight.LIGHTWEIGHT)
                    .cardOrder(i)
                    .title(false)
                    .canceled(false)
                    .build());
        }
        return fightEventRepository.save(event);   // cascade = ALL 이라 경기도 함께 저장된다
    }

    /**
     * 이 행이 없으면 픽 카운트 벌크 UPDATE 가 0건 매칭으로 끝나 <b>락을 아예 잡지 않는다</b>.
     * 그러면 direct 와 kafka 의 측정 결과가 똑같이 나와 비교 자체가 무의미해진다.
     */
    private void seedPickCounts(FightEvent event) {
        List<FightPickCount> counts = event.getFighterFightEvents().stream()
                .map(ffe -> FightPickCount.builder().fighterFightEvent(ffe).build())
                .toList();
        fightPickCountRepository.saveAll(counts);
    }

    private void seedUsersAndTokens() {
        try {
            Files.createDirectories(CSV_PATH.getParent());
            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(CSV_PATH))) {
                for (int i = 0; i < USER_COUNT; i++) {
                    String email = "load" + i + "@test.com";
                    userRepository.save(User.builder()
                            .email(email)
                            .nickname("load" + i)
                            .password("{noop}loadtest")   // 로그인 경로를 쓰지 않으므로 검증되지 않는다
                            .role("ROLE_USER")
                            .point(USER_POINT)
                            .earnedBetSucceedPoint(0)
                            .build());
                    String token = jwtUtil.createJwt(JwtCrateDto.toDto(
                            "access", email, "ROLE_USER", accessExpireMs, "LOCAL", false));
                    out.println(email + "," + token);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("부하 테스트용 토큰 CSV 작성 실패", e);
        }
    }

    private CardStartDateTimeInfo cardTime(LocalDate date, int hour, int minute) {
        return CardStartDateTimeInfo.builder().date(date).time(LocalTime.of(hour, minute)).build();
    }

    private Fighter fighter(String name, String koreanName) {
        return Fighter.builder()
                .name(name)
                .koreanName(koreanName)
                .nickname("LT")
                .height(180)        // CardFighterDto 가 int 로 받으므로 null 이면 NPE
                .reach(185)
                .weight(70.0)
                .ranking(1)
                .birthday(LocalDate.of(1995, 1, 1))
                .nationality(Country.US)
                .build();
    }

    private List<Long> fightIds(FightEvent event) {
        List<Long> ids = new ArrayList<>();
        event.getFighterFightEvents().forEach(ffe -> ids.add(ffe.getId()));
        return ids;
    }
}
