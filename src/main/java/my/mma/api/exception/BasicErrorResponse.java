package my.mma.api.exception;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BasicErrorResponse {

    private String errorCode;
    private HttpStatus status;
    private LocalDateTime timeStamp;

}