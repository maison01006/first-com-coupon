package com.custom.couponcore.service;

import com.custom.couponcore.component.DistributeLockExecutor;
import com.custom.couponcore.exception.CouponIssueException;
import com.custom.couponcore.model.Coupon;
import com.custom.couponcore.repository.redis.RedisRepository;
import com.custom.couponcore.repository.redis.dto.CouponIssueRequest;
import com.custom.couponcore.repository.redis.dto.CouponRedisEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.custom.couponcore.exception.ErrorCode.*;
import static com.custom.couponcore.util.CouponRedisUtils.getIssueRequestKey;
import static com.custom.couponcore.util.CouponRedisUtils.getIssueRequestQueueKey;


@RequiredArgsConstructor
@Service
public class AsyncCouponIssueServiceV1 {

    private final RedisRepository redisRepository;
    private final CouponIssueRedisService couponIssueRedisService;
    private final CouponIssueService couponIssueService;
    private final DistributeLockExecutor distributeLockExecutor;
    private final CouponCacheService couponCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public void issue(long couponId, long userId) throws CouponIssueException {
        CouponRedisEntity coupon = couponCacheService.getCouponCache(couponId);
        coupon.checkIssuableCoupon();
        distributeLockExecutor.execute("lock_%s".formatted(couponId),3000,3000,() -> {
            couponIssueRedisService.checkCouponIssueQuantity(coupon,userId);
            issueRequest(couponId, userId);
        });


    }

    private void issueRequest(long couponId, long userId) {
        CouponIssueRequest issueRequest = new CouponIssueRequest(couponId,userId);
        try {
            String value = objectMapper.writeValueAsString(issueRequest);
            redisRepository.sAdd(getIssueRequestKey(couponId), String.valueOf(userId));
            redisRepository.rPush(getIssueRequestQueueKey(),value);
        } catch (JsonProcessingException e) {
            throw new CouponIssueException(FAIL_COUPON_ISSUE_REQUEST,"input : %s".formatted(issueRequest));
        }
    }
}
