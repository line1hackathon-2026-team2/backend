package com.decisionlab.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        HttpStatus status = toHttpStatus(e.getErrorCode());
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(e.getErrorCode(), e.getMessage(), e.getCurrentState()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, message));
    }

    // JSON 문법 오류, enum에 없는 값(예: concernType = "JOB") 등 요청 본문을 읽지 못한 경우
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR,
                "요청 본문을 읽을 수 없습니다. JSON 형식과 enum 값을 확인해 주세요."));
    }

    // 경로/쿼리 값 타입 불일치 (예: sessionId가 UUID 형식이 아님)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR,
                "요청 값(" + e.getName() + ")의 형식이 올바르지 않습니다."));
    }

    private HttpStatus toHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_STATE_TRANSITION, RECONSIDER_LIMIT -> HttpStatus.CONFLICT;
            case SESSION_EXPIRED -> HttpStatus.GONE;
            case LLM_FAILED -> HttpStatus.BAD_GATEWAY;
        };
    }
}
