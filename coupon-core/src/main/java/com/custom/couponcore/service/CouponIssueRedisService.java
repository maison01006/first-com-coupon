package com.custom.couponcore.service;

import com.custom.couponcore.exception.CouponIssueException;
import com.custom.couponcore.repository.redis.RedisRepository;
import com.custom.couponcore.repository.redis.dto.CouponRedisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.custom.couponcore.exception.ErrorCode.DUPLICATED_COUPON_ISSUE;
import static com.custom.couponcore.exception.ErrorCode.INVAILD_COUPON_ISSUE_QUANTITY;
import static com.custom.couponcore.util.CouponRedisUtils.getIssueRequestKey;

@RequiredArgsConstructor
@Service
public class CouponIssueRedisService {

    private final RedisRepository redisRepository;
    public void checkCouponIssueQuantity(CouponRedisEntity couponRedisEntity, long userId) {
        if(!availableTotalIssueQuantity(couponRedisEntity.totalQuantity(),couponRedisEntity.id())) {
            throw new CouponIssueException(INVAILD_COUPON_ISSUE_QUANTITY,"발급 가능한 수량을 초과합니다. couponId : %s, userId : %s".formatted(couponRedisEntity.id(),userId));
        }
        if(!availableUserIssueQuantity(couponRedisEntity.id(),userId)) {
            throw new CouponIssueException(DUPLICATED_COUPON_ISSUE,"이미 발급 요청이 처리됐습니다. couponId : %s, userId : %s".formatted(couponRedisEntity.id(),userId));
        }
    }

    public boolean availableTotalIssueQuantity(Integer totalQuantity, long couponId) {
        if(totalQuantity == null) {
            return true;
        }
        String key = getIssueRequestKey(couponId);
        return totalQuantity > redisRepository.sCard(key);
    }

    public boolean availableUserIssueQuantity(long couponId, long userId) {
        String key = getIssueRequestKey(couponId);
        return !redisRepository.sIsMember(key,String.valueOf(userId));
    }
}
