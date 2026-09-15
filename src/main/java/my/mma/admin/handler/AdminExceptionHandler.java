package my.mma.admin.handler;

import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.BasicErrorResponse;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.ExceptionLogger;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice("my.mma.admin")
@Slf4j
public class AdminExceptionHandler {

    @ExceptionHandler
    public ModelAndView handleCustomException(
            CustomException e
    ) {
        return errorView(e.getErrorCode(), e);
    }

    @ExceptionHandler
    public ModelAndView handleHttpMessageConvertException(
            HttpMessageConversionException e
    ) {
        return errorView(ErrorCode.BAD_REQUEST_400, e);
    }

    //validation
    @ExceptionHandler
    public ModelAndView handleMethodArgumentException(
            MethodArgumentNotValidException e
    ) {
        return errorView(ErrorCode.VALIDATION_FAILED_400, e);
    }

    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ModelAndView handleNoResource(
            Exception e
    ) {
        return errorView(ErrorCode.URL_NOT_FOUND, e);
    }

    @ExceptionHandler
    public ModelAndView handleException(
            Exception e
    ) {
        return errorView(ErrorCode.SERVER_ERROR_500, e);
    }

    /** 로깅 정책은 API 쪽과 동일하게 ExceptionLogger 에 위임한다 (5xx=ERROR+스택, 4xx=WARN 1줄). */
    private ModelAndView errorView(ErrorCode errorCode, Exception e) {
        ExceptionLogger.log(log, errorCode, e);
        BasicErrorResponse response = BasicErrorResponse.builder()
                .errorCode(errorCode.name())
                .status(errorCode.getStatus())
                .timeStamp(LocalDateTime.now())
                .build();
        return new ModelAndView(
                "admin/error/error_view",
                Map.of(
                        "statusCode", response.getStatus(),
                        "errorCode", response.getErrorCode(),
                        "message", response.getErrorCode()
                ));
    }

}
