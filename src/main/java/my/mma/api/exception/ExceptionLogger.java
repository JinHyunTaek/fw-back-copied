package my.mma.api.exception;

import org.slf4j.Logger;

/**
 * 예외를 로그로 남기는 방식을 한 곳에 모은 유틸.
 * <p>
 * 기존에는 4xx(검증 실패·404·토큰 만료 등)까지 전부 ERROR 로, 그것도 요약/스택 두 줄로 남겼다.
 * 그 결과 app-error.log 의 대부분이 정상 트래픽이라 진짜 장애를 골라내기 어려웠다.
 * <p>
 * 정책
 * <ul>
 *   <li>5xx = 서버 잘못 → ERROR 1줄 + 스택트레이스</li>
 *   <li>4xx = 클라이언트 잘못 → WARN 1줄, 스택트레이스 없음 (app-info.log 로 분리됨)</li>
 * </ul>
 * errorCode 와 예외 클래스명을 메시지에 함께 실어, 로그를 grep 으로 집계할 수 있게 한다.
 */
public final class ExceptionLogger {

    private ExceptionLogger() {
    }

    public static void log(Logger log, ErrorCode errorCode, Exception e) {
        String exClass = e.getClass().getSimpleName();
        if (errorCode.getStatus().is5xxServerError()) {
            log.error("server error: errorCode={} exClass={} message={}",
                    errorCode.name(), exClass, e.getMessage(), e);
        } else {
            log.warn("client error: errorCode={} exClass={} message={}",
                    errorCode.name(), exClass, e.getMessage());
        }
    }
}
