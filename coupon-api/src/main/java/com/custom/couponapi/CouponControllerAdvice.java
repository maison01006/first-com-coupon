package com.custom.couponapi;

import com.custom.couponapi.controller.dto.CouponIssueResponseDto;
import com.custom.couponcore.exception.CouponIssueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class CouponControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CouponIssueException.class)
    public CouponIssueResponseDto couponIssueExceptionHandler(CouponIssueException exception) {
        return new CouponIssueResponseDto(false,exception.getErrorCode().message);
    }
}
