package com.decisionlab.global.error;

public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message, String currentState) {
    }

    public static ErrorResponse of(ErrorCode code, String message) {
        return of(code, message, null);
    }

    public static ErrorResponse of(ErrorCode code, String message, String currentState) {
        return new ErrorResponse(new ErrorBody(code.name(), message, currentState));
    }
}
