package com.hilkkeum.stamp.dto;

import java.util.List;

import com.hilkkeum.coupon.domain.WishCoupon;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스탬프 적립 결과. 승리면 awards 1명, 무승부면 2명.
 */
@Schema(description = "스탬프 적립 결과")
public record AwardStampResponse(
	@Schema(description = "게임 ID", example = "1") Long gameId,
	@Schema(description = "결산 결과 (WIN: 승자 1명 적립 / DRAW: 무승부, 벌칙 없이 두 사람 모두 적립)", example = "WIN") Result result,
	@Schema(description = "멤버별 적립 결과") List<MemberAward> awards
) {

	public enum Result {
		WIN, DRAW
	}

	@Schema(description = "멤버별 적립 결과")
	public record MemberAward(
		@Schema(description = "멤버 ID", example = "2") Long memberId,
		@Schema(description = "이번 요청으로 적립됐는지. 이미 처리된 게임이거나 소원권 보유 중(소멸)이면 false — 재시도해도 안전",
			example = "true") boolean awarded,
		@Schema(description = "현재 스탬프 수 (최대 3)", example = "3") int heartCount,
		@Schema(description = "이번 적립으로 소원권이 자동 발급됐는지", example = "true") boolean couponIssued,
		@Schema(description = "자동 발급된 소원권 ID (발급되지 않았으면 null)", example = "1", nullable = true) Long couponId
	) {

		public static MemberAward of(Long memberId, boolean awarded, int stampCount, WishCoupon issued) {
			return new MemberAward(memberId, awarded, stampCount, issued != null, issued == null ? null : issued.getId());
		}

		public static MemberAward notAwarded(Long memberId, int stampCount) {
			return new MemberAward(memberId, false, stampCount, false, null);
		}
	}
}
