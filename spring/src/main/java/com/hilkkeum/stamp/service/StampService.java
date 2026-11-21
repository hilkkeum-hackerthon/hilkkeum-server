package com.hilkkeum.stamp.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hilkkeum.common.exception.ApiException;
import com.hilkkeum.common.exception.ErrorCode;
import com.hilkkeum.coupon.domain.WishCoupon;
import com.hilkkeum.coupon.service.CouponService;
import com.hilkkeum.game.domain.Game;
import com.hilkkeum.game.repository.GameRepository;
import com.hilkkeum.room.domain.RoomMember;
import com.hilkkeum.room.repository.RoomMemberRepository;
import com.hilkkeum.stamp.dto.AwardStampResponse;
import com.hilkkeum.stamp.dto.StampsResponse;

import lombok.RequiredArgsConstructor;

/**
 * 스탬프 규칙
 * - 결산 승자에게 1개 적립, 최대 3개
 * - 3번째 스탬프를 얻었고 소원권이 없으면 소원권 자동 발급 (스탬프는 3개로 유지)
 * - 소원권 보유 중 승리하면 스탬프는 적립되지 않고 소멸
 * - 소원권을 사용하거나 만료되면 스탬프 0 (CouponService)
 */
@Service
@RequiredArgsConstructor
public class StampService {

	/** 스탬프 최대 보유량 = 소원권 1장에 필요한 수 */
	public static final int MAX_STAMPS = 3;

	private final GameRepository gameRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final CouponService couponService;

	/**
	 * 결산 승자에게 스탬프 1개 적립 (Node → Spring 내부 호출).
	 * - 결산이 확정된 게임이고, 요청한 승자가 DB에 확정된 승자와 같아야 한다 (아니면 409)
	 * - 게임당 1회만 처리(멱등): 같은 gameId 재요청은 awarded=false
	 * - 처리 기록 · 적립 · 자동 발급은 한 트랜잭션, 멤버 행을 잠가 동시 요청에도 소원권이 중복 발급되지 않음
	 */
	@Transactional
	public AwardStampResponse award(Long gameId, Long winnerMemberId) {
		Game game = gameRepository.findById(gameId)
			.orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));
		RoomMember winner = roomMemberRepository.findById(winnerMemberId)
			.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		if (!game.isWinnerConfirmed()) {
			throw new ApiException(ErrorCode.GAME_NOT_SETTLED);
		}
		Long confirmedWinnerMemberId = gameRepository.findMemberIdOfPlayer(gameId, game.getWinnerPlayerId())
			.orElseThrow(() -> new ApiException(ErrorCode.WINNER_MISMATCH));
		if (!confirmedWinnerMemberId.equals(winner.getId())
			|| !winner.getRoom().getId().equals(game.getRoomId())) {
			throw new ApiException(ErrorCode.WINNER_MISMATCH);
		}

		LocalDateTime now = LocalDateTime.now();
		if (gameRepository.markStampAwarded(gameId, now) != 1) {
			return AwardStampResponse.notAwarded(gameId, winnerMemberId, currentStamps(winnerMemberId));
		}
		couponService.expireOverdue(game.getRoomId(), now);

		RoomMember member = roomMemberRepository.findByIdForUpdate(winnerMemberId)
			.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		if (couponService.hasActiveCouponForUpdate(member.getId())) {
			// 소원권 보유 중 승리 → 스탬프 소멸
			return AwardStampResponse.notAwarded(gameId, winnerMemberId, member.getStampCount());
		}

		boolean awarded = member.addStamp(MAX_STAMPS);
		WishCoupon issued = member.hasFullStamps(MAX_STAMPS) ? couponService.issue(member, now) : null;
		return AwardStampResponse.of(gameId, winnerMemberId, awarded, member.getStampCount(), issued);
	}

	/** 보유 스탬프 및 소원권 보유 여부 */
	@Transactional
	public StampsResponse getStamps(Long memberId, Long roomId) {
		couponService.expireOverdue(roomId, LocalDateTime.now());
		return new StampsResponse(currentStamps(memberId), MAX_STAMPS, couponService.hasActiveCoupon(memberId));
	}

	private int currentStamps(Long memberId) {
		return roomMemberRepository.findById(memberId).map(RoomMember::getStampCount).orElse(0);
	}
}
