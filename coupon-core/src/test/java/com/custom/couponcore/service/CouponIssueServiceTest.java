package com.custom.couponcore.service;

import com.custom.couponcore.TestConfig;
import com.custom.couponcore.exception.CouponIssueException;
import com.custom.couponcore.exception.ErrorCode;
import com.custom.couponcore.model.Coupon;
import com.custom.couponcore.model.CouponIssue;
import com.custom.couponcore.model.CouponType;
import com.custom.couponcore.repository.mysql.CouponIssueJpaRepository;
import com.custom.couponcore.repository.mysql.CouponIssueRepository;
import com.custom.couponcore.repository.mysql.CouponJpaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CouponIssueServiceTest extends TestConfig {

    @Autowired
    CouponIssueService sut;

    @Autowired
    CouponIssueJpaRepository couponIssueJpaRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @BeforeEach
    void clean() {
        couponJpaRepository.deleteAllInBatch();
        couponIssueJpaRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("쿠폰 발급 내역이 존재하면 예외를 반환한다.")
    void alreadySaveCouponIssue() {
        // given
        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(1L)
                .userId(1L)
                .build();
        couponIssueJpaRepository.save(couponIssue);

        // when & then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class,() -> {
            sut.saveCouponIssue(couponIssue.getCouponId(),couponIssue.getUserId());
        });
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰 발급 내역이 존재하지 않는다면 쿠폰을 발급한다.")
    void saveCouponIssue() {
        // given
        long couponId = 1L;
        long userId = 1L;

        // when
        CouponIssue result = sut.saveCouponIssue(couponId,userId);

        // then
        Assertions.assertTrue(couponIssueJpaRepository.findById(result.getId()).isPresent());
    }

    @Test
    @DisplayName("발급 수량, 기한, 중복 발급 문제가 없다면 쿠폰을 발급한다.")
    void allOkCouponIssue() {
        //given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선찬숙 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();

        couponJpaRepository.save(coupon);

        //when
        sut.issue(coupon.getId(),userId);

        //then
        Coupon couponResult = couponJpaRepository.findById(coupon.getId()).get();
        Assertions.assertEquals(couponResult.getIssuedQuantity(),1);

        CouponIssue couponIssueResult = couponIssueRepository.findFirstCouponIssue(coupon.getId(),userId);
        Assertions.assertNotNull(couponIssueResult);
    }

    @Test
    @DisplayName("발급 수량에 문제가 있다면 예외를 반환한다.")
    void quantityErrorCouponIssue() {
        //given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선찬숙 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(100)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();

        couponJpaRepository.save(coupon);

        //when & then

        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class,() -> {
            sut.issue(coupon.getId(),userId);
        });
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.INVAILD_COUPON_ISSUE_QUANTITY);
    }


    @Test
    @DisplayName("발급 기한에 문제가 있다면 예외를 반환한다.")
    void dateErrorCouponIssue() {
        //given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선찬숙 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(2))
                .dateIssueEnd(LocalDateTime.now().minusDays(1))
                .build();

        couponJpaRepository.save(coupon);

        //when & then

        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class,() -> {
            sut.issue(coupon.getId(),userId);
        });
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("중복 발급에 문제가 있다면 예외를 반환한다.")
    void duplicatedErrorCouponIssue() {
        //given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선찬숙 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();

        couponJpaRepository.save(coupon);


        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(coupon.getId())
                .userId(userId)
                .build();

        couponIssueJpaRepository.save(couponIssue);

        //when & then

        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class,() -> {
            sut.issue(coupon.getId(),userId);
        });
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰이 존재하지 않는다면 예외를 반환한다.")
    void NoneCouponCouponIssue() {

        //given
        long userId = 1L;
        long couponId = 1L;

        // when & then

        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class,() ->{
            sut.issue(couponId,userId);
        });
        Assertions.assertEquals(exception.getErrorCode(),ErrorCode.COUPON_NOT_EXIST);

    }
}