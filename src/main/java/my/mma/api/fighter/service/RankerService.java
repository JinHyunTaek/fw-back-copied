package my.mma.api.fighter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.fighter.dto.RankersPerCategory;
import my.mma.api.global.redis.prefix.RedisKeyPrefix;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RankerService {

    private final RedisUtils<RankersPerCategory> rankersDtoRedisUtils;

    public RankersPerCategory rankers(RankersPerCategory.RankingCategory category) {
        return rankersDtoRedisUtils.getData(RedisKeyPrefix.RANKERS_KEY_PREFIX.getPrefix() + category.name());
    }

}
