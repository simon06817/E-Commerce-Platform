package com.ecommerce.platform.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converts exceptions thrown by controllers and services into the unified
 * {@link Result} envelope.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Result<Void>> handleValidationException(Exception e) {
        String msg = "请求参数错误";
        if (e instanceof BindException be && be.getBindingResult().getAllErrors().size() > 0) {
            msg = be.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (e instanceof MethodArgumentNotValidException me
                && me.getBindingResult().getAllErrors().size() > 0) {
            msg = me.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        return ResponseEntity.badRequest()
                .body(Result.error(ResultCode.BAD_REQUEST.getCode(), msg));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Result<Void>> handleAuthorizationDenied(AuthorizationDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(ResultCode.FORBIDDEN));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(ResultCode.NOT_FOUND));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(statusFor(e.getResultCode()))
                .body(Result.error(e.getResultCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException e) {
        log.error("runtime exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ResultCode.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("unexpected exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ResultCode.INTERNAL_SERVER_ERROR));
    }

    private HttpStatus statusFor(ResultCode resultCode) {
        return switch (resultCode) {
            case BAD_REQUEST, INVALID_ROLE -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED, USERNAME_OR_PASSWORD_ERROR -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, PRODUCT_NOT_OWNED, REFUNDED_PRODUCT_CANNOT_REVIEW -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, USER_NOT_EXIST, PRODUCT_NOT_EXIST, CART_ITEM_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
            case STOCK_INSUFFICIENT, CATEGORY_HAS_CHILDREN, USER_ALREADY_EXISTS,
                    REVIEW_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case REVIEW_NOT_ALLOWED, BUYER_PROFILE_INCOMPLETE -> HttpStatus.BAD_REQUEST;
            case INTERNAL_SERVER_ERROR, SUCCESS -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
