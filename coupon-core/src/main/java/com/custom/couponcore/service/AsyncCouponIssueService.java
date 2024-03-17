package com.custom.couponcore.service;

import com.custom.couponcore.exception.CouponIssueException;
import com.custom.couponcore.repository.redis.RedisRepository;
import com.custom.couponcore.repository.redis.dto.CouponRedisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;


@RequiredArgsConstructor
@Service
public class AsyncCouponIssueService {

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
