package my.mma.api.global.logaop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Around("@annotation(loggable)")
    public Object doLog(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (loggable.includeArgsLog()) {
            log.info("[{}] start args={}", joinPoint.getSignature(), args);
        } else {
            log.info("[{}] start ", joinPoint.getSignature());
        }
        Object returnVal = joinPoint.proceed();
        log.info("[{}] return value={}", joinPoint.getSignature(), returnVal);
        return returnVal;
    }
}
