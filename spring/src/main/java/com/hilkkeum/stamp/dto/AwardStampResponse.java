package com.hilkkeum.stamp.dto;

/**
 * @param awarded    이번 요청으로 적립됐으면 true, 이미 적립된 게임이면 false (재시도해도 안전)
 * @param stampCount 승자의 현재 스탬프 수
 */
public record AwardStampResponse(Long gameId, Long winnerMemberId, boolean awarded, int stampCount) {
}
