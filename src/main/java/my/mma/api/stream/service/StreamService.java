package my.mma.api.stream.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StreamService {

    private final RedisUtils<CurrentEventDto> redisUtils;

    public CurrentEventDto getWeeklyEvent() {
        return redisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
    }

}
