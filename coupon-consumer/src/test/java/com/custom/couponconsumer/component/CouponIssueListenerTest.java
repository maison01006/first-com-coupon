package com.custom.couponconsumer.component;

import com.custom.couponconsumer.TestConfig;
import com.custom.couponcore.repository.redis.RedisRepository;
import com.custom.couponcore.service.CouponIssueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@Import(CouponIssueListener.class)
class CouponIssueListenerTest extends TestConfig {

    @Autowired
    CouponIssueListener sut;

    @Autowired
    RedisTemplate<String,String> redisTemplate;

    @Autowired
    RedisRepository redisRepository;

    @MockBean
    CouponIssueService couponIssueService;

    @BeforeEach
    void clear() {
        Collection<String> redisKeys = redisTemplate.keys("*");
        redisTemplate.delete(redisKeys);
    }

    @Test
    @DisplayName("쿠폰 발급 큐에 처리 대상이 없다면 발급을 하지 않는다.")
    void emptyQueue() throws JsonProcessingException {
        //given

        //when
        sut.issue();
        //then
        verify(couponIssueService,never()).issue(anyLong(),anyLong());
    }

    @Test
    @DisplayName("쿠폰 발급 큐에 처리 대상이 있다면 발급한다.")
    void haveQueue() throws JsonProcessingException {
        //given
        long couponId = 1;
        long userId = 1;
        int totalQuantity = Integer.MAX_VALUE;
        redisRepository.issueRequest(couponId,userId,totalQuantity);

        //when
        sut.issue();
        //then
        verify(couponIssueService,times(1)).issue(couponId,userId);
    }

    @Test
    @DisplayName("쿠폰 발급 요청 순서에 맞게 처리된다.")
    void sequenceQueue() throws JsonProcessingException {
        //given
        long couponId = 1;
        long userId = 1;
        long user2Id = 2;
        long user3Id = 3;
        int totalQuantity = Integer.MAX_VALUE;
        redisRepository.issueRequest(couponId,userId,totalQuantity);
        redisRepository.issueRequest(couponId,user2Id,totalQuantity);
        redisRepository.issueRequest(couponId,user3Id,totalQuantity);

        //when
        sut.issue();

        //then
        InOrder inOrder = Mockito.inOrder(couponIssueService);
        inOrder.verify(couponIssueService,times(1)).issue(couponId,userId);
        inOrder.verify(couponIssueService,times(1)).issue(couponId,user2Id);
        inOrder.verify(couponIssueService,times(1)).issue(couponId,user3Id);
    }

}