//package my.mma.api.game.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import my.mma.api.game.dto.FighterNamePair;
//import my.mma.api.game.dto.GameType;
//import my.mma.api.game.dto.ImageGameResponse;
//import my.mma.api.global.s3.service.S3ImgService;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Set;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ImageGameService implements GameService<ImageGameResponse>{
//
//    private final S3ImgService s3Service;
//    private final GameCacheService gameCacheService;
//
//    @Override
//    public GameType getType() {
//        return GameType.IMAGE;
//    }
//
//    @Override
//    public List<ImageGameResponse> generateGame(boolean isNormal) {
//        Set<FighterNamePair> names = isNormal
//                ? gameCacheService.popularFighterNamePairs()
//                : gameCacheService.allFighterNamePairs();
//        List<FighterNamePair> namePairs = new ArrayList<>(names);
//
//        List<ImageGameResponse> imageGames = new ArrayList<>();
//        int currentQuestionCnt = 0;
//        Collections.shuffle(namePairs);
//        for (FighterNamePair namePair : namePairs) {
//            String headshotUrl = s3Service.generateFighterHeadshotUrlOrNull(namePair.name());
//            if (headshotUrl != null) {
//                if (currentQuestionCnt % 4 == 0) {
//                    if (currentQuestionCnt == 20)
//                        break;
//                    currentQuestionCnt++;
//                    imageGames.add(ImageGameResponse.builder()
//                            .name(namePair.koreanName())
//                            .answer(headshotUrl)
//                            .wrongSelections(new ArrayList<>())
//                            .build());
//                } else {
//                    imageGames.get((currentQuestionCnt / 4))
//                            .wrongSelections().add(headshotUrl);
//                    currentQuestionCnt++;
//                }
//            }
//        }
//        return imageGames;
//    }
//
//}
