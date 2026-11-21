package com.hilkkeum.stamp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hilkkeum.common.auth.AuthMember;
import com.hilkkeum.common.auth.CurrentMember;
import com.hilkkeum.stamp.dto.StampsResponse;
import com.hilkkeum.stamp.service.StampService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stamps")
@RequiredArgsConstructor
public class StampController {

	private final StampService stampService;

	/** 보유 스탬프 및 교환 가능 여부 조회 (Header: Authorization: Bearer {memberToken}) */
	@GetMapping
	public StampsResponse getStamps(@CurrentMember AuthMember me) {
		return stampService.getStamps(me.memberId());
	}
}
