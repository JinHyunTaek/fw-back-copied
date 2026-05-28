package my.mma.api.user.service;

import lombok.RequiredArgsConstructor;
import my.mma.api.alert.repository.AlertRepository;
import my.mma.api.alert.repository.UserPreferencesRepository;
import my.mma.api.bet.repository.BetCardRepository;
import my.mma.api.bet.repository.BetRepository;
import my.mma.api.fighter.repository.UserFighterRatingRepository;
import my.mma.api.inquiry.repository.InquiryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserReferencedEntitiesDeleteService {

    private final AlertRepository alertRepository;
    private final BetCardRepository betCardRepository;
    private final BetRepository betRepository;
    private final InquiryRepository inquiryRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserFighterRatingRepository userFighterRatingRepository;

    @Transactional
    public void deleteUserReferencedEntities(Long userId){
        userPreferencesRepository.deleteUserPreferencesByUserId(userId);
        alertRepository.deleteAllByUserId(userId);
        betCardRepository.deleteBetCardsByUserId(userId);
        betRepository.deleteAllByUserId(userId);
        inquiryRepository.deleteAllByUserId(userId);
        userFighterRatingRepository.deleteAllByUserId(userId);
    }

}
