package com.custom.couponapi.controller;

import com.custom.couponapi.controller.dto.CouponIssueRequestDto;
import com.custom.couponapi.controller.dto.CouponIssueResponseDto;
import com.custom.couponapi.service.CouponIssueRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequiredArgsConstructor
@Controller
public class CouponIssueController {

    private final CouponIssueRequestService couponIssueRequestService;

    @PostMapping("/v1/issue")
    public CouponIssueResponseDto issueV1(@RequestBody CouponIssueRequestDto body) {
        couponIssueRequestService.issueRequestV1(body);
        return new CouponIssueResponseDto(true,null);
    }

    @PostMapping("/v1/issue-async")
    public CouponIssueResponseDto asyncASDIssueV1(@RequestBody CouponIssueRequestDto body) {
        couponIssueRequestService.asyncIssueRequestV1(body);
        return new CouponIssueResponseDto(true,null);
    }
}
