package my.mma.api.game.service;

import my.mma.api.exception.CustomException;
import my.mma.api.fighter.dto.RankersPerCategory;
import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.game.dto.FighterNamePair;
import my.mma.api.game.dto.NameGameResponse;
import my.mma.api.global.redis.utils.RedisUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static my.mma.api.exception.ErrorCode.SERVER_ERROR_500;
import static my.mma.api.game.dto.NameGameCategory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 무작위(shuffle/Random)에 의존하는 "정답 값"은 검증하지 않고,
 * 무작위와 무관하게 항상 성립하는 분기/예외/계약만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NameGameService 질문 생성 분기")
class NameGameServiceTest {

    @Mock
    private RedisUtils<RankersPerCategory> rankersRedisUtils;
    @Mock
    private FighterRepository fighterRepository;
    @Mock
    private FighterFightEventRepository fighterFightEventRepository;
    @Mock
    private GameCacheService gameCacheService;

    @InjectMocks
    private NameGameService nameGameService;

    @Nested
    @DisplayName("OPPONENT")
    class Opponent {

        @Test
        @DisplayName("names()에 포함된 파이터들이 모두 승리 기록이 2개 미만이면 SERVER_ERROR_500")
        void allFightersHaveLessThanTwoWins_throws() {
            Set<FighterNamePair> names = Set.of(pair("n1", "코1"), pair("n2", "코2"));
            when(fighterFightEventRepository.findCompletedByWinnerName(anyString())).thenReturn(List.of());

            assertThatThrownBy(() -> nameGameService.generateNameQuestion(OPPONENT, names))
                    .isInstanceOfSatisfying(CustomException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(SERVER_ERROR_500));
        }

        @Test
        @DisplayName("승리 2개 이상인 파이터가 있으면 그 파이터로 문제 생성 (상대 2명, 오답 3개)")
        void fighterWithTwoWins_buildsQuestion() {
            // given
            FighterNamePair target = pair("target", "타깃");
            Set<FighterNamePair> names = Set.of(target,
                    pair("o1", "상대1"),
                    pair("o2", "상대2"),
                    pair("o3", "상대3"));

            // when
            when(fighterFightEventRepository.findCompletedByWinnerName(anyString())).thenReturn(List.of());
            // production이 wins를 Collections.shuffle 하므로 가변 리스트로 스텁
            when(fighterFightEventRepository.findCompletedByWinnerName("target"))
                    .thenReturn(new ArrayList<>(List.of(
                            ffeWithLoser(korFighter("패배자1")),
                            ffeWithLoser(korFighter("패배자2")))));
            NameGameResponse resp = nameGameService.generateNameQuestion(OPPONENT, names);

            // then
            assertThat(resp.nameGameCategory()).isEqualTo(OPPONENT);
            assertThat(resp.answer()).isEqualTo("타깃");
            assertThat(resp.opponents()).containsExactlyInAnyOrder("패배자1", "패배자2");
            assertThat(resp.wrongSelections()).hasSize(3); // target 제외한 나머지 koreanName
        }
    }

    @Nested
    @DisplayName("RECORD")
    class Record {

        @Test
        @DisplayName("전적이 있는 파이터가 없으면 SERVER_ERROR_500")
        void noFighterWithRecord_throws() {
            Set<FighterNamePair> names = Set.of(pair("n1", "코1"), pair("n2", "코2"));
            when(fighterRepository.findByNameAndFightRecordIsNotNull(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> nameGameService.generateNameQuestion(RECORD, names))
                    .isInstanceOfSatisfying(CustomException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(SERVER_ERROR_500));
        }

        @Test
        @DisplayName("전적이 있는 파이터가 있으면 그 파이터로 문제 생성")
        void fighterWithRecord_buildsQuestion() {
            // given
            FightRecord record = FightRecord.builder().win(20).draw(0).loss(1).build();
            Set<FighterNamePair> names = Set.of(pair("recName", "레코드킹"), pair("x", "엑스"));

            // when
            when(fighterRepository.findByNameAndFightRecordIsNotNull(anyString())).thenReturn(Optional.empty());
            when(fighterRepository.findByNameAndFightRecordIsNotNull("recName"))
                    .thenReturn(Optional.of(fighter("recName", "레코드킹", null, record)));

            NameGameResponse resp = nameGameService.generateNameQuestion(RECORD, names);

            // then
            assertThat(resp.nameGameCategory()).isEqualTo(RECORD);
            assertThat(resp.answer()).isEqualTo("레코드킹");
            assertThat(resp.fightRecord()).isEqualTo(record);
        }
    }

    @Nested
    @DisplayName("NICKNAME")
    class Nickname {

        @Test
        @DisplayName("닉네임이 있는 파이터가 없으면 SERVER_ERROR_500")
        void noFighterWithNickname_throws() {
            Set<FighterNamePair> names = Set.of(pair("n1", "코1"), pair("n2", "코2"));
            when(fighterRepository.findByNameAndNicknameIsNotNull(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> nameGameService.generateNameQuestion(NICKNAME, names))
                    .isInstanceOfSatisfying(CustomException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(SERVER_ERROR_500));
        }

        @Test
        @DisplayName("닉네임이 있는 파이터가 있으면 그 파이터로 문제 생성")
        void fighterWithNickname_buildsQuestion() {
            // given
            Set<FighterNamePair> names = Set.of(pair("nickName", "닉주인"), pair("x", "엑스"));

            // when
            when(fighterRepository.findByNameAndNicknameIsNotNull(anyString())).thenReturn(Optional.empty());
            when(fighterRepository.findByNameAndNicknameIsNotNull("nickName"))
                    .thenReturn(Optional.of(fighter("nickName", "닉주인", "더 코리안 좀비", null)));
            NameGameResponse resp = nameGameService.generateNameQuestion(NICKNAME, names);

            // then
            assertThat(resp.nameGameCategory()).isEqualTo(NICKNAME);
            assertThat(resp.answer()).isEqualTo("닉주인");
            assertThat(resp.nickname()).isEqualTo("더 코리안 좀비");
        }
    }

    @Nested
    @DisplayName("COUNTRY")
    class Country {

        @Test
        @DisplayName("[현재 동작] 국적 조건을 만족하는 파이터가 없으면 가드가 없어 NoSuchElementException 발생")
        void noFighter_throwsNoSuchElement() {
            Set<FighterNamePair> names = Set.of(pair("a", "에이"));
            when(fighterRepository.findByNameInAndNationalityIsNotNull(anyList())).thenReturn(List.of());

            // OPPONENT/RECORD/NICKNAME과 달리 빈 결과 가드(SERVER_ERROR_500)가 없음 → 개선 포인트
            assertThatThrownBy(() -> nameGameService.generateNameQuestion(COUNTRY, names))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // --- helpers ---

    private static FighterNamePair pair(String name, String koreanName) {
        return new FighterNamePair(name, koreanName);
    }

    private static Fighter korFighter(String koreanName) {
        return Fighter.builder().koreanName(koreanName).build();
    }

    private static Fighter fighter(String name, String koreanName, String nickname, FightRecord record) {
        return Fighter.builder().name(name).koreanName(koreanName).nickname(nickname).fightRecord(record).build();
    }

    private static FighterFightEvent ffeWithLoser(Fighter loser) {
        return FighterFightEvent.builder().loser(loser).build();
    }
}
