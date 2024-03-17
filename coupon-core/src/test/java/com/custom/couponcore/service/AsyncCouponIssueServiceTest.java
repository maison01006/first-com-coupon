package com.custom.couponcore.service;

import com.custom.couponcore.TestConfig;
import com.custom.couponcore.exception.CouponIssueException;
import com.custom.couponcore.exception.ErrorCode;
import com.custom.couponcore.model.Coupon;
import com.custom.couponcore.model.CouponType;
import com.custom.couponcore.repository.mysql.CouponJpaRepository;
import com.custom.couponcore.repository.redis.dto.CouponIssueRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.IntStream;

import static com.custom.couponcore.exception.ErrorCode.*;
import static com.custom.couponcore.util.CouponRedisUtils.getIssueRequestKey;
import static com.custom.couponcore.util.CouponRedisUtils.getIssueRequestQueueKey;

class AsyncCouponIssueServiceTest extends TestConfig{

    @Autowired
    AsyncCouponIssueService sut;

    @Autowired
    RedisTemplate<String,String> redisTemplate;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @BeforeEach
    void clear() {
        Collection<String> redisKeys = redisTemplate.keys("*");
        redisTemplate.delete(redisKeys);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰이 존재하지 않는다면 예외를 반환한다.")
    void noneCouponIssue() {
        //given
        long couponId = 1;
        long userId = 1;

        //when
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class,() -> {
            sut.issue(couponId,userId);
        });

        //then
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.COUPON_NOT_EXIST);
    }

    @Test
    @DisplayName("쿠폰 발급 - 발급 가능 수량이 존재하지 않는다면 예외를 반환한다.")
    void zeroCouponIssueQuantity() {
        //given
        long userId = 1000;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        IntStream.range(0, coupon.getTotalQuantity()).forEach(idx -> {
            redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()),String.valueOf(idx));
        });
        //when & then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class,() ->{
            sut.issue(coupon.getId(),userId);
        });
        Assertions.assertEquals(exception.getErrorCode(), INVAILD_COUPON_ISSUE_QUANTITY);
    }

    @Test
    @DisplayName("쿠폰 발급 - 이미 발급된 유저라면 예외를 반환한다.")
    void dupliCouponIssueQuantity() {
        //given
        long userId = 1;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()),String.valueOf(userId));

        //when & then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class,() ->{
            sut.issue(coupon.getId(),userId);
        });
        Assertions.assertEquals(exception.getErrorCode(), DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰 발급 - 발급 기한이 지난 쿠폰이라면 예외를 반환한다.")
    void notRangeCouponIssueDate() {
        //given
        long userId = 1;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(2))
                .dateIssueEnd(LocalDateTime.now().minusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        //when & then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class,() ->{
            sut.issue(coupon.getId(),userId);
        });
        Assertions.assertEquals(exception.getErrorCode(), INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰 발급을 기록한다.")
    void couponIssueSaveRedisSet() {
        //given
        long userId = 1;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        //when

        sut.issue(coupon.getId(),userId);

        // then
        Boolean isSaved = redisTemplate.opsForSet().isMember(getIssueRequestKey(coupon.getId()),String.valueOf(userId));
        Assertions.assertTrue(isSaved);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰 발급 요청이 성공하면 쿠폰 발급 큐에 적제된다.")
    void couponIssueSaveRedisQueue() throws JsonProcessingException {
        //given
        long userId = 1;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        CouponIssueRequest request = new CouponIssueRequest(coupon.getId(),userId);
        //when

        sut.issue(coupon.getId(),userId);

        // then
        String isSaved = redisTemplate.opsForList().leftPop(getIssueRequestQueueKey());
        Assertions.assertEquals(new ObjectMapper().writeValueAsString(request),isSaved);
    }
}