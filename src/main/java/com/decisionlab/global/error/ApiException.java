package com.decisionlab.global.error;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String currentState;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public ApiException(ErrorCode errorCode, String message, String currentState) {
        super(message);
        this.errorCode = errorCode;
        this.currentState = currentState;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCurrentState() {
        return currentState;
    }
}
