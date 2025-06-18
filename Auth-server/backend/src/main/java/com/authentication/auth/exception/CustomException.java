package com.authentication.auth.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorType errorType;
    private final String customMessage;

    public CustomException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.customMessage = errorType.getMessage();
    }

    public CustomException(ErrorType errorType, String customMessage) {
        super(customMessage);
        this.errorType = errorType;
        this.customMessage = customMessage;
    }

    public CustomException(ErrorType errorType, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorType = errorType;
        this.customMessage = customMessage;
    }
}
