package my.mma.api.fightevent.store;

import lombok.RequiredArgsConstructor;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;

@Component
@RequiredArgsConstructor
public class CurrentEventStore {

    private final RedisUtils<CurrentEventDto> currentEventRedisUtils;
    private final ReentrantLock lock = new ReentrantLock();

    public void mutate(UnaryOperator<CurrentEventDto> mutator){
        lock.lock();
        try{
            CurrentEventDto cur = currentEventRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
            if(cur == null)
                return;
            CurrentEventDto next = mutator.apply(cur);
            if(next != null)
                currentEventRedisUtils.updateData(RedisKey.CURRENT_EVENT.getKey(), next);
        }finally {
            lock.unlock();
        }
    }

}
