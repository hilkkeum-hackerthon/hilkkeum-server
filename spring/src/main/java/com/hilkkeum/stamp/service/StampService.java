package com.hilkkeum.stamp.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hilkkeum.common.exception.ApiException;
import com.hilkkeum.common.exception.ErrorCode;
import com.hilkkeum.coupon.service.CouponService;
import com.hilkkeum.game.domain.Game;
import com.hilkkeum.game.repository.GameRepository;
import com.hilkkeum.room.domain.RoomMember;
import com.hilkkeum.room.repository.RoomMemberRepository;
import com.hilkkeum.stamp.dto.AwardStampResponse;
import com.hilkkeum.stamp.dto.StampsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StampService {

	/** 스탬프 최대 보유량 (= 소원권 교환에 필요한 수). 3개면 더 적립하지 않는다 */
	public static final int MAX_STAMPS = CouponService.STAMPS_PER_COUPON;

	private final GameRepository gameRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final CouponService couponService;

	/**
	 * 결산 승자에게 스탬프 1개 적립.
	 * - 결산이 확정된 게임이고, 요청한 승자가 DB에 확정된 승자와 같아야 한다 (아니면 409)
	 * - 게임당 1회만 처리(멱등): 같은 gameId 재요청은 awarded=false
	 * - 보유량이 3개면 적립하지 않음(awarded=false). 소원권은 자동 발급하지 않는다
	 * - 처리 기록(stamp_awarded_at)과 적립은 한 트랜잭션
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

		boolean firstTime = gameRepository.markStampAwarded(gameId, LocalDateTime.now()) == 1;
		boolean awarded = firstTime && roomMemberRepository.addStampUnderCap(winnerMemberId, MAX_STAMPS) == 1;

		return new AwardStampResponse(gameId, winnerMemberId, awarded, currentStamps(winnerMemberId));
	}

	/** 보유 스탬프 및 교환 가능 여부 */
	@Transactional
	public StampsResponse getStamps(Long memberId, Long roomId) {
		int stampCount = currentStamps(memberId);
		boolean exchangeable = stampCount >= MAX_STAMPS && !couponService.hasActiveCoupon(memberId, roomId);
		return new StampsResponse(stampCount, CouponService.STAMPS_PER_COUPON, exchangeable);
	}

	private int currentStamps(Long memberId) {
		return roomMemberRepository.findById(memberId).map(RoomMember::getStampCount).orElse(0);
	}
}
