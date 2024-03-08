package com.custom.couponcore.service;

import com.custom.couponcore.component.DistributeLockExecutor;
import com.custom.couponcore.exception.CouponIssueException;
import com.custom.couponcore.repository.redis.RedisRepository;
import com.custom.couponcore.repository.redis.dto.CouponIssueRequest;
import com.custom.couponcore.repository.redis.dto.CouponRedisEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.custom.couponcore.exception.ErrorCode.FAIL_COUPON_ISSUE_REQUEST;
import static com.custom.couponcore.util.CouponRedisUtils.getIssueRequestKey;
import static com.custom.couponcore.util.CouponRedisUtils.getIssueRequestQueueKey;


@RequiredArgsConstructor
@Service
public class AsyncCouponIssueServiceV2 {

    private final RedisRepository redisRepository;
    private final CouponCacheService couponCacheService;

    public void issue(long couponId, long userId) throws CouponIssueException {
        CouponRedisEntity coupon = couponCacheService.getCouponLocalCache(couponId);
        coupon.checkIssuableCoupon();
        issueRequest(couponId, userId,coupon.totalQuantity());
    }

    private void issueRequest(long couponId, long userId,Integer totalIssueQuantity) {
        redisRepository.issueRequest(couponId, userId, Objects.requireNonNullElse(totalIssueQuantity, Integer.MAX_VALUE));
    }
}
