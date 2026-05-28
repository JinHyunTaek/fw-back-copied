package my.mma.api.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode){
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    // for more specific error message (ex) internal server error logging)
    public CustomException(ErrorCode errorCode, String errorMessage){
        super(errorMessage);
        this.errorCode = errorCode;
    }

}