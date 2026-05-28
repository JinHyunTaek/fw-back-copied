package my.mma.api.home;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class HomeService {

    private final RedisUtils<CurrentEventDto> redisUtils;

    public HomeScreenDto home(){
        CurrentEventDto currentEventDto = redisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
        if(currentEventDto == null)
            return null;
        return HomeScreenDto.toDto(currentEventDto);
    }

}
