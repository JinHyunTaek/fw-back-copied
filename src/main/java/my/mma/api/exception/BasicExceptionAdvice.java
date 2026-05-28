package my.mma.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

import static my.mma.api.exception.ErrorCode.*;

@RestControllerAdvice("my.mma.api")
@Slf4j
public class BasicExceptionAdvice {

    @ExceptionHandler
    public ResponseEntity<BasicErrorResponse> handleCustomException(
            CustomException e
    ) {
        if(e.getMessage() != null)
            log.error("ex = {}, detail error message = {}",e.getErrorCode().name(),e.getMessage());
        return throwException(e.getErrorCode(), e);
    }

    @ExceptionHandler
    public ResponseEntity<BasicErrorResponse> handleHttpMessageConvertException(
            HttpMessageConversionException e
    ) {
        return throwException(BAD_REQUEST_400, e);
    }

    //valid
    @ExceptionHandler
    public ResponseEntity<BasicErrorResponse> handleMethodArgumentException(
            MethodArgumentNotValidException e
    ) {
        return throwException(VALIDATION_FAILED_400, e);
    }

    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<BasicErrorResponse> handleNoResource(
            Exception e
    ) {
        return throwException(URL_NOT_FOUND, e);
    }

    @ExceptionHandler
    public ResponseEntity<BasicErrorResponse> handleException(
            Exception e
    ) {
        return throwException(SERVER_ERROR_500, e);
    }

    private ResponseEntity<BasicErrorResponse> throwException(
            ErrorCode errorCode, Exception e
    ){
        log.error("ex = {}", e.getMessage());
        log.error("detail message = ", e);
        BasicErrorResponse response = BasicErrorResponse.builder()
                .errorCode(errorCode.name())
                .status(errorCode.getStatus())
                .timeStamp(LocalDateTime.now())
                .build();
        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }

}

