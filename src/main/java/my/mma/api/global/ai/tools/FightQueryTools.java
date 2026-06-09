//package my.mma.api.global.ai.tools;
//
//import lombok.RequiredArgsConstructor;
//import my.mma.api.fightevent.dto.CurrentEventDto;
//import my.mma.api.fightevent.entity.FighterFightEvent;
//import my.mma.api.fightevent.repository.FighterFightEventRepository;
//import my.mma.api.global.ai.dto.CurrentFightEventChatTool;
//import my.mma.api.global.ai.dto.FighterProfileChatTool;
//import my.mma.api.global.redis.key.RedisKey;
//import my.mma.api.global.redis.utils.RedisUtils;
//import org.springframework.ai.tool.annotation.Tool;
//import org.springframework.ai.tool.annotation.ToolParam;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class FightQueryTools {
//
//    private final FighterFightEventRepository fighterFightEventRepository;
//    private final RedisUtils<CurrentEventDto> currentEventDtoRedisUtils;
//
//    @Tool(description = "이번 주 열리는 경기 정보 조회")
//    public CurrentFightEventChatTool getCurrentEventChatTool(){
//        return CurrentFightEventChatTool.of(currentEventDtoRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey()));
//    }
//
//    @Tool(description = "특정 선수의 프로필(커리어 스탯)과 최근 전적을 조회한다. " +
//            "이번 주 경기 예측을 위한 부가 기능이므로, " +
//            "이번 주 열리는 경기 정보에 포함된 선수에 대한 선수 이름이 아니면 조회하지 않는다.")
//    public FighterProfileChatTool getFighterChatTool(@ToolParam(description = "선수 이름(영문)") String fighterName){
//        List<FighterFightEvent> fights = fighterFightEventRepository.findByFighterName(fighterName);
//        return FighterProfileChatTool.of(fighterName, fights);
//    }
//
//}
