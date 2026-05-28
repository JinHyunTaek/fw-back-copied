package my.mma.api.fighter.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.alert.repository.AlertRepository;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.fighter.dto.FighterDetailDto;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fighter.dto.FighterRatingDto;
import my.mma.api.fighter.dto.FighterRatingRequest;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.entity.UserFighterRating;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fighter.repository.UserFighterRatingRepository;
import my.mma.api.fightevent.dto.FightEventDto.FighterFightEventDto;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FighterService {

    private final FighterRepository fighterRepository;
    private final FighterFightEventRepository fighterFightEventRepository;
    private final UserFighterRatingRepository userFighterRatingRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public FighterDetailDto detail(String email, Long fighterId) {
        Fighter fighter = getFighter(fighterId);
        User user = getUser(email);
        boolean isAlertExists = alertRepository.existsByUserAndAlertTargetAndTargetId(user, AlertTarget.FIGHTER, fighterId);
        List<FighterFightEvent> fighterFightEvents = fighterFightEventRepository.
                findByFighterIdAndYear(fighter.getId(), LocalDate.now().getYear());
        List<FighterFightEventDto> fighterFightEventDtos = fighterFightEvents.stream()
                .map(FighterFightEventDto::toDto)
                .toList();
//                .peek(ffe -> {
//                    ffe.getWinner().setHeadshotUrl(s3Service.generateFighterHeadshotUrl(ffe.getWinner().getName()));
//                    ffe.getLoser().setHeadshotUrl(s3Service.generateFighterHeadshotUrl(ffe.getLoser().getName()));
//                }).toList();
//        String bodyUrl = s3Service.generateFighterBodyUrl(fighter.getName());
        UserFighterRating myRating = userFighterRatingRepository.findByUserIdAndFighterId(user.getId(), fighterId)
                .orElse(null);
        return FighterDetailDto.toDto(fighter, fighterFightEventDtos,
                isAlertExists, myRating != null ? myRating.getRating() : 0);
    }

    public List<FighterFightEventDto> getFighterFightEventsByYear(Long fighterId, int year) {
        return fighterFightEventRepository.findByFighterIdAndYear(fighterId, year)
                .stream().map(FighterFightEventDto::toDto)
//                .peek(ffe -> {
//                    ffe.getWinner().setHeadshotUrl(s3Service.generateFighterHeadshotUrl(ffe.getWinner().getName()));
//                    ffe.getLoser().setHeadshotUrl(s3Service.generateFighterHeadshotUrl(ffe.getLoser().getName()));
//                })
                .toList();
    }


    public Page<FighterDto> search(String keyword, Pageable pageable) {
        Page<Fighter> fighters = fighterRepository.searchByNameOrKoreanName(keyword, pageable);
        //                    fighterDto.setHeadshotUrl(s3Service.generateFighterHeadshotUrl(fighterDto.getName()));
        return fighters.map(FighterDto::toDto);
    }

    @Transactional
    public void rate(String email, FighterRatingRequest ratingDto) {
        User user = getUser(email);
        Fighter fighter = getFighterWithLock(ratingDto.fighterId());
        Optional<UserFighterRating> userFighterRating = userFighterRatingRepository.findByUserIdAndFighterId
                (user.getId(), ratingDto.fighterId());
        userFighterRating.ifPresentOrElse(
                r -> r.updateRating(ratingDto.rating()),
                () -> userFighterRatingRepository.save(
                        UserFighterRating.builder()
                                .fighter(fighter)
                                .user(user)
                                .rating(ratingDto.rating())
                                .build())
        );
        double avgRating = userFighterRatingRepository.findAvgRatingByFighterId(ratingDto.fighterId());
        fighter.updateAvgRating(avgRating);
    }

    public Page<FighterRatingDto> getAvgRatingRank(Pageable pageable) {
        if (pageable.getPageNumber() >= 10)
            return new PageImpl<>(List.of(), pageable, 0);
        return fighterRepository.findByAvgRatingGreaterThan(-1, pageable)
                .map(FighterRatingDto::toDto);
//                .map(fighter -> toDto(fighter, s3Service.generateFighterHeadshotUrl(fighter.getName())));
    }

    private @NonNull Fighter getFighter(Long fighterId) {
        return fighterRepository.findById(fighterId).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_FIGHTER_CONFIGURED_400)
        );
    }

    private @NonNull Fighter getFighterWithLock(Long fighterId) {
        return fighterRepository.findByIdWithLock(fighterId).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_FIGHTER_CONFIGURED_400)
        );
    }

    private @NonNull User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_USER_FOUND_400)
        );
    }

}
