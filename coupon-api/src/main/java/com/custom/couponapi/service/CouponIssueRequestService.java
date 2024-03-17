package com.custom.couponapi.service;

import com.custom.couponapi.controller.dto.CouponIssueRequestDto;
import com.custom.couponcore.component.DistributeLockExecutor;
import com.custom.couponcore.service.AsyncCouponIssueService;
import com.custom.couponcore.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class CouponIssueRequestService {

    private final AsyncCouponIssueService asyncCouponIssueService;
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());


    public void asyncIssueRequest(CouponIssueRequestDto requestDto) {
        asyncCouponIssueService.issue(requestDto.couponId(), requestDto.userId());
        log.info("쿠폰 발급 완료. couponId : %s, userId : %s".formatted(requestDto.couponId(),requestDto.userId()));
    }
}
