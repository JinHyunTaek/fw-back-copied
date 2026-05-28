package my.mma.admin.handler;

import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.BasicErrorResponse;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
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
        if (e.getMessage() != null)
            log.error("ex = {}, detail error message = {}", e.getErrorCode().name(), e.getMessage());
        else
            log.error("ex = {}", e.getErrorCode().name());
        BasicErrorResponse response = BasicErrorResponse.builder()
                .errorCode(e.getErrorCode().name())
                .status(e.getErrorCode().getStatus())
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

    @ExceptionHandler
    public ModelAndView handleHttpMessageConvertException(
            HttpMessageConversionException e
    ) {
        log.error("ex = ", e);
        BasicErrorResponse response = BasicErrorResponse.builder()
                .errorCode(ErrorCode.BAD_REQUEST_400.name())
                .status(ErrorCode.BAD_REQUEST_400.getStatus())
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

    //validation
    @ExceptionHandler
    public ModelAndView handleMethodArgumentException(
            MethodArgumentNotValidException e
    ) {
        log.error("ex = {}", e.getMessage());
        BasicErrorResponse response = BasicErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_FAILED_400.name())
                .status(ErrorCode.VALIDATION_FAILED_400.getStatus())
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

    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ModelAndView handleNoResource(
            Exception e
    ) {
        log.error("ex = {}", e.getMessage());
        BasicErrorResponse response = BasicErrorResponse.builder()
                .errorCode(ErrorCode.URL_NOT_FOUND.name())
                .status(ErrorCode.URL_NOT_FOUND.getStatus())
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

    @ExceptionHandler
    public ModelAndView handleException(
            Exception e
    ) {
        log.error("ex = {}", e.getMessage());
        log.error("detail message = ", e);
        BasicErrorResponse response = BasicErrorResponse.builder()
                .errorCode(ErrorCode.SERVER_ERROR_500.name())
                .status(ErrorCode.SERVER_ERROR_500.getStatus())
                .timeStamp(LocalDateTime.now())
                .build();
        return new ModelAndView(
                "admin/error/error_view",
                Map.of(
                        "statusCode", response.getStatus(),
                        "errorCode",response.getErrorCode(),
                        "message",response.getErrorCode()
                ));
    }

}
